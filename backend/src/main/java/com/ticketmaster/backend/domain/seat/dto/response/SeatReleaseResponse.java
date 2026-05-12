package com.ticketmaster.backend.domain.seat.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 해제 응답
 *
 * { "releasedSeatIds": [11, 12] }
 *
 * - 멱등성: 본인 점유가 아니거나 이미 해제된 좌석은 응답 리스트에서 제외
 * - 빈 리스트여도 200 OK (요청 자체는 성공)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SeatReleaseResponse {

    private List<Long> releasedSeatIds;

    public static SeatReleaseResponse of(List<Long> releasedSeatIds) {
        return new SeatReleaseResponse(releasedSeatIds);
    }
}
