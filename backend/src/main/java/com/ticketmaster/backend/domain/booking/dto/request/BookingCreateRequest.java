package com.ticketmaster.backend.domain.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BookingCreateRequest {

    @NotNull(message = "matchId: 회차 ID는 필수입니다.")
    private Long matchId;

    @NotEmpty(message = "seatIds: 좌석은 1개 이상 선택해야 합니다.")
    private List<Long> seatIds;
}