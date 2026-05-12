package com.ticketmaster.backend.domain.seat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 해제 요청
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SeatReleaseRequest {

    @NotEmpty(message = "해제할 좌석이 없습니다.")
    private List<Long> seatIds;
}
