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
    private final LocalDateTime allowedAt;      // ALLOWED 로 승격된 시각 (WAITING 시 null)
    private final LocalDateTime entryDeadline;  // 좌석 점유 데드라인 (= allowedAt + sessionSeconds, WAITING 시 null)

    /**
     * WAITING 일 때 응답
     */
    public static QueueEnterResponse waiting(
            String token,
            long queueNumber,
            long remainingAhead,
            long estimatedWaitSeconds,
            LocalDateTime enteredAt
    ) {
        return new QueueEnterResponse(
                token, "WAITING",
                queueNumber, remainingAhead,
                estimatedWaitSeconds, enteredAt,
                null, null  // allowedAt, entryDeadline
        );
    }

    // 즉시 ALLOWED 응답 — burst 게이트 통과 시
    // queueNumber/remainingAhead/estimatedWaitSeconds 는 0 으로 채움
    // 프론트는 status 필드로 분기
    public static QueueEnterResponse allowed(
            String token,
            LocalDateTime enteredAt,
            LocalDateTime allowedAt,
            LocalDateTime entryDeadline
    ) {
        return new QueueEnterResponse(
                token, "ALLOWED",
                0L, 0L, 0L,
                enteredAt,
                allowedAt, entryDeadline
        );
    }
}
