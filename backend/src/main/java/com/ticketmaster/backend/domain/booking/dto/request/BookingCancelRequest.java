package com.ticketmaster.backend.domain.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "예매 취소 요청")
@Getter
@NoArgsConstructor
public class BookingCancelRequest {

    // TODO: 추후 사용자에게 취소 사유를 받는다 하면 사용할 것
    // @NotBlank(message = "취소 사유는 필수입니다.")
    @Schema(description = "취소 사유 (선택)", example = "일정 변경")
    private String cancelReason;
}