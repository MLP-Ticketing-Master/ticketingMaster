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
 * <p>결제 플로우:
 * <pre>
 * 1. 사용자가 "결제하기" 클릭
 * 2. Payment.ready() → DB에 READY 상태로 저장
 * 3. 프론트에서 PG(토스 등) 결제창 오픈
 * 4. PG 콜백 수신
 *    - 성공: markSuccess(paymentKey) → SUCCESS
 *    - 실패: markFailed()           → FAILED
 * </pre>
 */
@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Booking ↔ Payment = 1:1, Payment 쪽이 FK 보유 */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "payment_key", length = 100)
    private String paymentKey;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    /** READY 상태의 Payment 생성 (결제창 띄우기 전 호출) */
    public static Payment ready(Booking booking, PaymentMethod method, int amount) {
        Payment p = new Payment();
        p.booking = booking;
        p.method = method;
        p.amount = amount;
        p.status = PaymentStatus.READY;
        return p;
    }

    /** PG 승인 성공 처리 (READY → SUCCESS) */
    public void markSuccess(String paymentKey) {
        if (this.status != PaymentStatus.READY) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    /** PG 승인 실패/취소 처리 (→ FAILED) */
    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
    }
}
