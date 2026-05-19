package com.ticketmaster.backend.domain.payment.dto.response;

import com.ticketmaster.backend.domain.payment.entity.Payment;
import com.ticketmaster.backend.domain.payment.entity.PaymentMethod;
import com.ticketmaster.backend.domain.payment.entity.PaymentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /payments/{paymentId} 응답 바디
 * 결제 결과 + 환불 정보까지 종합 보여줌 (마이페이지용)
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentDetailResponse {

    private final Long paymentId;
    private final Long bookingId;
    private final PaymentMethod method;
    private final int amount;
    private final PaymentStatus status;
    private final LocalDateTime paidAt;
    private final String failureReason;
    private final LocalDateTime canceledAt;
    private final LocalDateTime refundedAt;
    private final Integer refundAmount;
    private final LocalDateTime createdAt;

    public static PaymentDetailResponse from(Payment p) {
        return new PaymentDetailResponse(
                p.getId(),
                p.getBooking().getId(),
                p.getMethod(),
                p.getAmount(),
                p.getStatus(),
                p.getPaidAt(),
                p.getFailureReason(),
                p.getCanceledAt(),
                p.getRefundedAt(),
                p.getRefundAmount(),
                p.getCreatedAt()
        );
    }
}
