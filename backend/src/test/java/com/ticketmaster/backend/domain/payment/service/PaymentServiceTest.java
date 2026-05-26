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
import com.ticketmaster.backend.domain.payment.entity.PaymentStatus;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import com.ticketmaster.backend.domain.payment.toss.TossApiException;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentResponse;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentsClient;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.queue.service.QueueService;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 결제 승인 / 조회 비즈니스 로직 단위 테스트
 * - 토스 API 는 Mock 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("결제 승인 단위 테스트")
class PaymentServiceTest {

    @Mock
    BookingRepository bookingRepository;
    @Mock
    PaymentRepository paymentRepository;
    @Mock
    TossPaymentsClient tossClient;
    @Mock
    PaymentFailureRecorder paymentFailureRecorder;
    @Mock
    QueueService queueService;

    @InjectMocks
    PaymentService paymentService;

    private User user;
    private Booking booking;
    private Seat seat;

    @BeforeEach
    void setUp() {
        // given - 공통 픽스처 (정상 결제 가능한 상태)
        // JPA 엔티티는 protected 생성자라 BeanUtils 로 인스턴스화
        user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", 1L);

        seat = BeanUtils.instantiateClass(Seat.class);
        ReflectionTestUtils.setField(seat, "status", SeatStatus.RESERVED);
        ReflectionTestUtils.setField(seat, "reservedBy", 1L);
        ReflectionTestUtils.setField(seat, "reservedUntil", LocalDateTime.now().plusMinutes(7));

        BookingSeat bs = BeanUtils.instantiateClass(BookingSeat.class);
        ReflectionTestUtils.setField(bs, "seat", seat);

        Match match = BeanUtils.instantiateClass(Match.class);
        ReflectionTestUtils.setField(match, "id", 100L);

        booking = BeanUtils.instantiateClass(Booking.class);
        ReflectionTestUtils.setField(booking, "id", 10L);
        ReflectionTestUtils.setField(booking, "user", user);
        ReflectionTestUtils.setField(booking, "match", match);
        ReflectionTestUtils.setField(booking, "status", BookingStatus.PENDING);
        ReflectionTestUtils.setField(booking, "totalPrice", 50000);
        ReflectionTestUtils.setField(booking, "bookingSeats", List.of(bs));
    }

    private PaymentConfirmRequest request(String paymentKey, int amount) {
        PaymentConfirmRequest req = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(req, "bookingId", 10L);
        ReflectionTestUtils.setField(req, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(req, "orderId", "order-1");
        ReflectionTestUtils.setField(req, "amount", amount);
        return req;
    }

    private TossPaymentResponse tossSuccess(String paymentKey, int amount) {
        TossPaymentResponse res = new TossPaymentResponse();
        ReflectionTestUtils.setField(res, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(res, "orderId", "order-1");
        ReflectionTestUtils.setField(res, "status", "DONE");
        ReflectionTestUtils.setField(res, "method", "카드");
        ReflectionTestUtils.setField(res, "totalAmount", amount);
        ReflectionTestUtils.setField(res, "approvedAt", OffsetDateTime.now());
        return res;
    }

    @Test
    @DisplayName("정상 결제 → Payment(SUCCESS) + Booking(CONFIRMED) + Seat(SOLD)")
    void 정상_결제() {
        // given
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));
        given(tossClient.confirm("pk-1", "order-1", 50000)).willReturn(tossSuccess("pk-1", 50000));

        // when
        PaymentResponse res = paymentService.confirm(request("pk-1", 50000), 1L);

        // then
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD);
        assertThat(res.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Booking 없이 결제 → BOOKING_NOT_FOUND")
    void Booking_없음() {
        // given
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_NOT_FOUND);
    }

    @Test
    @DisplayName("타인 예매 결제 → FORBIDDEN")
    void 타인_예매() {
        // given
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));

        // when & then - userId=2 는 booking.user.id=1 과 불일치
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("CONFIRMED 상태 예매 결제 → BOOKING_NOT_PENDING")
    void CONFIRMED_상태() {
        // given
        ReflectionTestUtils.setField(booking, "status", BookingStatus.CONFIRMED);
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_NOT_PENDING);
    }

    @Test
    @DisplayName("amount 불일치 → INVALID_PAYMENT_AMOUNT")
    void 금액_불일치() {
        // given
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));

        // when & then - booking.totalPrice=50000 이지만 amount=99999 전달
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 99999), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    @Test
    @DisplayName("토스 승인 실패 → PaymentFailureRecorder 호출 + Booking(PENDING 유지)")
    void 토스_승인_실패() {
        // given
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));
        willThrow(new TossApiException("카드 한도 초과"))
                .given(tossClient).confirm(anyString(), anyString(), any(Integer.class));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_FAILED);

        // 별도 트랜잭션으로 Payment FAILED 기록되어 호출자 rollback 영향 안 받음
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(paymentFailureRecorder).recordFailure(
                eq(booking), eq("pk-1"), eq("order-1"), eq(50000), anyString());
    }

    @Test
    @DisplayName("멱등성 — 같은 paymentKey 재호출 시 기존 결과 반환")
    void 멱등성() {
        // given - 이미 SUCCESS 상태 Payment 존재
        Payment existing = Payment.success(booking, "pk-1", "order-1", 50000,
                PaymentMethod.CARD, LocalDateTime.now());
        ReflectionTestUtils.setField(existing, "id", 99L);
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.of(existing));

        // when
        PaymentResponse res = paymentService.confirm(request("pk-1", 50000), 1L);

        // then
        assertThat(res.getPaymentId()).isEqualTo(99L);
        assertThat(res.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // then - 멱등성의 핵심: 토스 호출 X, 저장 X
        verify(tossClient, never()).confirm(anyString(), anyString(), any(Integer.class));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("좌석 AVAILABLE 상태 → SEAT_RESERVATION_EXPIRED + 토스 호출 안 됨")
    void 좌석_만료_AVAILABLE() {
        // given
        ReflectionTestUtils.setField(seat, "status", SeatStatus.AVAILABLE);
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SEAT_RESERVATION_EXPIRED);
    }

    @Test
    @DisplayName("다른 사용자 점유 좌석 (reservedBy 불일치) → SEAT_RESERVATION_EXPIRED")
    void 좌석_타인_점유() {
        // given
        ReflectionTestUtils.setField(seat, "reservedBy", 999L);
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SEAT_RESERVATION_EXPIRED);
    }

    @Test
    @DisplayName("reservedUntil 과거 → SEAT_RESERVATION_EXPIRED")
    void 좌석_만료_시간() {
        // given
        ReflectionTestUtils.setField(seat, "reservedUntil", LocalDateTime.now().minusMinutes(1));
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SEAT_RESERVATION_EXPIRED);
    }

    @Test
    @DisplayName("좌석 SOLD 상태 → SEAT_RESERVATION_EXPIRED")
    void 좌석_SOLD() {
        // given
        ReflectionTestUtils.setField(seat, "status", SeatStatus.SOLD);
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SEAT_RESERVATION_EXPIRED);
    }

    @Test
    @DisplayName("토스 승인 성공 + PaymentRepository.save 예외 → cancel 호출")
    void 보상_DB저장_실패() {
        // given
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));
        given(tossClient.confirm(anyString(), anyString(), any(Integer.class)))
                .willReturn(tossSuccess("pk-1", 50000));
        willThrow(new RuntimeException("DB 일시 장애"))
                .given(paymentRepository).save(any(Payment.class));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);

        verify(tossClient).cancel(eq("pk-1"), anyString());
    }

    @Test
    @DisplayName("이중 실패 (cancel 도 실패) → 로그 ERROR + 500 응답")
    void 이중_실패() {
        // given
        given(paymentRepository.findByPaymentKey("pk-1")).willReturn(Optional.empty());
        given(bookingRepository.findForPayment(10L)).willReturn(Optional.of(booking));
        given(tossClient.confirm(anyString(), anyString(), any(Integer.class)))
                .willReturn(tossSuccess("pk-1", 50000));
        willThrow(new RuntimeException("DB 일시 장애"))
                .given(paymentRepository).save(any(Payment.class));
        willThrow(new TossApiException("환불도 실패"))
                .given(tossClient).cancel(eq("pk-1"), anyString());

        // then & then - 이중 실패여도 외부엔 500 응답
        assertThatThrownBy(() -> paymentService.confirm(request("pk-1", 50000), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("본인 결제 조회 → 200 + 전체 필드 포함")
    void 본인_조회() {
        // given
        Payment payment = Payment.success(booking, "pk-1", "order-1", 50000,
                PaymentMethod.CARD, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 99L);
        given(paymentRepository.findByIdWithBooking(99L)).willReturn(Optional.of(payment));

        // when
        PaymentDetailResponse res = paymentService.getPaymentDetail(99L, 1L);

        // then
        assertThat(res.getPaymentId()).isEqualTo(99L);
        assertThat(res.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("타인 결제 조회 → FORBIDDEN")
    void 타인_조회() {
        // given
        Payment payment = Payment.success(booking, "pk-1", "order-1", 50000,
                PaymentMethod.CARD, LocalDateTime.now());
        given(paymentRepository.findByIdWithBooking(99L)).willReturn(Optional.of(payment));

        // when & then - userId=2 가 booking.user.id=1 과 불일치
        assertThatThrownBy(() -> paymentService.getPaymentDetail(99L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 paymentId → PAYMENT_NOT_FOUND")
    void 존재하지_않는_결제() {
        // given
        given(paymentRepository.findByIdWithBooking(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.getPaymentDetail(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }
}