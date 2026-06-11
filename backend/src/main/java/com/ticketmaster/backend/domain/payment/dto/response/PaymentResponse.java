package com.ticketmaster.backend.domain.payment.dto.response;

import com.ticketmaster.backend.domain.payment.entity.Payment;
import com.ticketmaster.backend.domain.payment.entity.PaymentMethod;
import com.ticketmaster.backend.domain.payment.entity.PaymentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * POST /payments/confirm 응답 바디
 * 결제 완료 후 클라이언트에게 내려가는 결과 요약
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentResponse {

    private final Long paymentId;
    private final Long bookingId;
    private final PaymentMethod method;
    private final int amount;
    private final PaymentStatus status;
    private final LocalDateTime paidAt;
    private final String failureReason;

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getFailureReason()
        );
    }
}
