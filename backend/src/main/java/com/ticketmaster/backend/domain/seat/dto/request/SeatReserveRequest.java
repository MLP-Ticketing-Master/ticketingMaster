package com.ticketmaster.backend.domain.seat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 일괄 점유 요청
 * - maxTicketsPerUser 상한 검증은 Service 에서
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SeatReserveRequest {

    @NotEmpty(message = "좌석을 선택해주세요.")
    private List<Long> seatIds;
}
