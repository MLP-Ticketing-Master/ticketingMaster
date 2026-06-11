package com.ticketmaster.backend.domain.seat.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 좌석 점유 성공 응답
 *
 * { "reservedSeatIds": [11, 12],
 *   "reservedUntil":   "2026-05-11T18:00:00",
 *   "totalPrice":      300000 }
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SeatReserveResponse {

    private List<Long> reservedSeatIds;
    private LocalDateTime reservedUntil;
    private int totalPrice;

    public static SeatReserveResponse of(List<Long> reservedSeatIds,
                                         LocalDateTime reservedUntil,
                                         int totalPrice) {
        return new SeatReserveResponse(reservedSeatIds, reservedUntil, totalPrice);
    }
}
