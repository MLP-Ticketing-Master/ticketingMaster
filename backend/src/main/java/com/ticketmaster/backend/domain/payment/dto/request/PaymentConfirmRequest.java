package com.ticketmaster.backend.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /payments/confirm 요청 바디
 * 토스 위젯에서 받은 paymentKey + orderId + amount 를 그대로 백엔드에 전달
 */
@Schema(description = "결제 승인 요청 (토스 위젯 응답값 전달)")
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @Schema(description = "예매 ID", example = "1")
    @NotNull(message = "예매 ID는 필수입니다")
    private Long bookingId;

    @Schema(description = "토스에서 발급한 결제 키", example = "tviva20240101...")
    @NotBlank(message = "결제 키는 필수입니다")
    private String paymentKey;

    @Schema(description = "주문 ID", example = "order_abc123")
    @NotBlank(message = "주문 ID는 필수입니다")
    private String orderId;

    @Schema(description = "결제 금액 (원)", example = "55000")
    @Positive(message = "금액은 0보다 커야 합니다")
    private int amount;
}
