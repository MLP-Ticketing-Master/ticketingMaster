package com.ticketmaster.backend.domain.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookingCancelRequest {

    // TODO: 추후 사용자에게 취소 사유를 받는다 하면 사용할 것
    // @NotBlank(message = "취소 사유는 필수입니다.")
    private String cancelReason;
}