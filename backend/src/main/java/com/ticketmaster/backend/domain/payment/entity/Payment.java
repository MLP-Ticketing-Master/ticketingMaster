package com.ticketmaster.backend.domain.payment.entity;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.global.common.BaseEntity;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 엔티티
 *
 * 토스페이먼츠 위젯 흐름
 *  1) 프론트가 토스 위젯에서 결제 처리 → paymentKey 발급
 *  2) 백엔드가 POST /payments/confirm 호출 (paymentKey + orderId + amount)
 *  3) 백엔드가 토스 승인 API 호출 → 응답에 따라 SUCCESS / FAILED Payment 생성
 *  4) 환불 시 토스 cancel API 호출 + Payment status CANCELED 전환
 */
@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    @SequenceGenerator(name = "payment_seq", sequenceName = "PAYMENT_SEQ", allocationSize = 50)
    private Long id;

    /** Booking ↔ Payment = 1:1, Payment 쪽이 FK 보유 */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /** 토스 발급 결제 키 — 멱등성 검증용, UNIQUE 제약 */
    @Column(name = "payment_key", nullable = false, length = 100, unique = true)
    private String paymentKey;

    /** 토스 주문 ID — 우리가 생성해서 토스에 넘긴 값 */
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    /** 실패 시 토스 응답의 사유 메시지 */
    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    /** Payment 가 CANCELED 상태로 전환된 시각 */
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    /** 환불 완료 시각 (토스 cancel API 응답 시각) */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /** 실제 환불 금액 — 전액 환불 + 부분 환불 (수수료 차감) 모두 관리 */
    @Column(name = "refund_amount")
    private Integer refundAmount;

    /**
     * 토스 승인 성공 응답을 받아 Payment 생성
     * status=SUCCESS, paidAt=토스 응답의 approvedAt
     */
    public static Payment success(Booking booking, String paymentKey, String orderId,
                                  int amount, PaymentMethod method, LocalDateTime approvedAt) {
        Payment p = new Payment();
        p.booking = booking;
        p.paymentKey = paymentKey;
        p.orderId = orderId;
        p.amount = amount;
        p.method = method;
        p.status = PaymentStatus.SUCCESS;
        p.paidAt = approvedAt;
        return p;
    }

    /**
     * 토스 승인 실패 시 Payment 생성
     * status=FAILED, failureReason 기록
     */
    public static Payment failed(Booking booking, String paymentKey, String orderId,
                                 int amount, String failureReason) {
        Payment p = new Payment();
        p.booking = booking;
        p.paymentKey = paymentKey;
        p.orderId = orderId;
        p.amount = amount;
        p.status = PaymentStatus.FAILED;
        p.failedAt = LocalDateTime.now();
        p.failureReason = failureReason;
        return p;
    }

    /**
     * 환불 처리 — status=CANCELED + 환불 시각 / 금액 기록
     * 호출 위치: PaymentService.refund() — 토스 cancel API 응답 성공 후
     */
    public void refund(int refundAmount) {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        LocalDateTime now = LocalDateTime.now();
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = now;
        this.refundedAt = now;
        this.refundAmount = refundAmount;
    }
}
