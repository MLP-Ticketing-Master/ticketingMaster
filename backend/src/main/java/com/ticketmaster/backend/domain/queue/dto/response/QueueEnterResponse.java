package com.ticketmaster.backend.domain.queue.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 대기열 진입 응답 DTO
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueEnterResponse {

    private final String queueToken;            // 대기열 토큰
    private final String status;                // 현재 상태 (WAITING / ALLOWED / EXPIRED)
    private final long queueNumber;             // 내 순번 (1-based, ZRANK + 1)
    private final long remainingAhead;          // 내 앞에 남은 인원 수
    private final long estimatedWaitSeconds;    // 예상 대기 시간 (초)
    private final LocalDateTime enteredAt;      // 대기열 진입 시각

    /**
     * 진입 시점에는 항상 status=WAITING 이라 그 부분을 자동으로 채워주는 정적 메서드
     */
    public static QueueEnterResponse of(
            String token,
            long queueNumber,
            long remainingAhead,
            long estimatedWaitSeconds,
            LocalDateTime enteredAt
    ) {
        return new QueueEnterResponse(
                token, "WAITING",
                queueNumber, remainingAhead,
                estimatedWaitSeconds, enteredAt
        );
    }
}
