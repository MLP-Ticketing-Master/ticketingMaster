package com.ticketmaster.backend.domain.payment.service;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingSeat;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.payment.dto.request.PaymentConfirmRequest;
import com.ticketmaster.backend.domain.payment.dto.response.PaymentDetailResponse;
import com.ticketmaster.backend.domain.payment.dto.response.PaymentResponse;
import com.ticketmaster.backend.domain.payment.entity.Payment;
import com.ticketmaster.backend.domain.payment.entity.PaymentMethod;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import com.ticketmaster.backend.domain.payment.toss.TossApiException;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentResponse;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentsClient;
import com.ticketmaster.backend.domain.queue.service.QueueService;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 결제 비즈니스 로직
 * <p>
 * - confirm(): POST /payments/confirm 흐름 + DB 실패 시 자동 환불 보상
 * - refund(): 외부 호출용 환불 메서드 (예매 취소에서 사용)
 * - getPaymentDetail() : 결제 상세 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentsClient tossClient;
    private final PaymentFailureRecorder paymentFailureRecorder;
    private final QueueService queueService;

    /**
     * 결제 승인 처리
     * 1) 멱등성 체크 (paymentKey 로 기존 Payment 조회)
     * 2) 사전 검증 (Booking 소유 / PENDING / amount / 좌석 재검증)
     * 3) 토스 승인 API 호출
     * 4) 성공 → DB 트랜잭션 (Booking CONFIRMED + Seat SOLD + Payment SUCCESS)
     * 실패 → Payment FAILED 만 기록
     * 5) DB 트랜잭션 실패 시 토스 cancel API 호출하여 자동 환불 보상
     * 6) 결제 완료 → admission 회수 (다음 예매는 다시 대기열부터, 회수 실패는 결제 자체엔 영향 없음)
     */
    @Transactional
    public PaymentResponse confirm(PaymentConfirmRequest req, Long userId) {
        // 멱등성 — 같은 paymentKey 재호출은 기존 결과 반환
        Optional<Payment> existing = paymentRepository.findByPaymentKey(req.getPaymentKey());
        if (existing.isPresent()) {
            log.info("[Payment] 멱등성 hit paymentKey={}", req.getPaymentKey());
            return PaymentResponse.from(existing.get());
        }

        Booking booking = bookingRepository.findForPayment(req.getBookingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        // 사전 검증 (토스 호출 전)
        validateBooking(booking, userId, req.getAmount());
        validateSeatsStillReserved(booking, userId);

        // 토스 승인 호출
        TossPaymentResponse tossRes;
        try {
            tossRes = tossClient.confirm(req.getPaymentKey(), req.getOrderId(), req.getAmount());
        } catch (TossApiException e) {
            // 토스 승인 실패 — 별도 트랜잭션(REQUIRES_NEW)으로 Payment FAILED 기록, Booking PENDING 유지
            paymentFailureRecorder.recordFailure(booking, req.getPaymentKey(),
                    req.getOrderId(), req.getAmount(), e.getMessage());
            log.warn("[Payment] 토스 승인 실패 paymentKey={}", req.getPaymentKey());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        // 승인 성공 — DB 트랜잭션 일괄 처리
        // 토스 approvedAt 은 OffsetDateTime (UTC 또는 +09:00 등 토스 응답 오프셋에 따라 다름)
        // → Asia/Seoul 타임존으로 정규화 후 LocalDateTime 으로 변환해 한국 시간으로 일관 저장
        LocalDateTime paidAt = tossRes.getApprovedAt() != null
                ? tossRes.getApprovedAt().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
                : LocalDateTime.now();
        Payment payment = Payment.success(
                booking, tossRes.getPaymentKey(), tossRes.getOrderId(),
                tossRes.getTotalAmount(), PaymentMethod.fromToss(tossRes.getMethod()),
                paidAt
        );

        try {
            paymentRepository.save(payment);
            booking.confirm();
            for (BookingSeat bs : booking.getBookingSeats()) {
                bs.getSeat().markAsSold();
            }
        } catch (RuntimeException e) {
            log.error("[Payment] DB 저장 실패 — 토스 환불 보상 시작 paymentKey={}", req.getPaymentKey(), e);
            compensateRefund(req.getPaymentKey());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 결제 완료 = 1회 예매 종료 → admission 회수 (다음 예매는 다시 대기열부터)
        // 회수 실패는 결제 자체엔 영향 X — TTL 로 자연 만료되므로 로그만 남기고 통과
        try {
            queueService.expireUserAdmission(booking.getMatch().getId(), userId);
        } catch (RuntimeException e) {
            log.warn("[Payment] admission 회수 실패 — TTL 로 자연 만료 예상 userId={} matchId={}",
                    userId, booking.getMatch().getId(), e);
        }

        log.info("[Payment] 결제 완료 paymentId={} bookingId={}", payment.getId(), booking.getId());
        return PaymentResponse.from(payment);
    }

    /**
     * 외부 호출용 환불 — 예매 취소에서 호출
     * 1) 토스 cancel API 호출
     * 2) Payment 상태 갱신 (CANCELED + refundedAt + refundAmount)
     */
    @Transactional
    public void refund(Payment payment, String cancelReason, int refundAmount) {
        try {
            tossClient.cancel(payment.getPaymentKey(), cancelReason);
        } catch (TossApiException e) {
            log.warn("[Payment] 환불 호출 실패 paymentKey={}", payment.getPaymentKey(), e);
            throw new BusinessException(ErrorCode.TOSS_API_ERROR);
        }
        payment.refund(refundAmount);
        log.info("[Payment] 환불 완료 paymentId={} refundAmount={}", payment.getId(), refundAmount);
    }

    // 결제 상세 조회
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetail(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findByIdWithBooking(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getBooking().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return PaymentDetailResponse.from(payment);
    }

    // ------ 헬퍼 -----------------------------------------

    private void validateBooking(Booking booking, Long userId, int amount) {
        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_PENDING);
        }
        if (booking.getTotalPrice() != amount) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
    }

    private void validateSeatsStillReserved(Booking booking, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        for (BookingSeat bs : booking.getBookingSeats()) {
            Seat seat = bs.getSeat();
            if (seat.getStatus() != SeatStatus.RESERVED
                    || !userId.equals(seat.getReservedBy())
                    || seat.getReservedUntil() == null
                    || !now.isBefore(seat.getReservedUntil())) {
                throw new BusinessException(ErrorCode.SEAT_RESERVATION_EXPIRED);
            }
        }
    }

    /**
     * 토스 승인 성공 후 DB 트랜잭션 실패 시 자동 환불 보상
     * 이중 실패 (환불도 실패) 시 로그 ERROR + 수동 처리 필요
     */
    private void compensateRefund(String paymentKey) {
        try {
            tossClient.cancel(paymentKey, "DB 저장 실패로 자동 환불");
            log.info("[Payment] 보상 환불 성공 paymentKey={}", paymentKey);
        } catch (TossApiException e) {
            log.error("[Payment] 이중 실패 — 수동 환불 필요 paymentKey={}", paymentKey, e);
        }
    }
}
