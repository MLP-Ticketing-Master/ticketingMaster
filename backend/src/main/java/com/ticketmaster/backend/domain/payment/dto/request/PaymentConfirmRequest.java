package com.ticketmaster.backend.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /payments/confirm 요청 바디
 * 토스 위젯에서 받은 paymentKey + orderId + amount 를 그대로 백엔드에 전달
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @NotNull(message = "예매 ID는 필수입니다")
    private Long bookingId;

    @NotBlank(message = "결제 키는 필수입니다")
    private String paymentKey;

    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;

    @Positive(message = "금액은 0보다 커야 합니다")
    private int amount;
}
