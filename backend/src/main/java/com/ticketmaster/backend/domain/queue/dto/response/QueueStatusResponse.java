package com.ticketmaster.backend.domain.queue.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 대기열 상태 조회 응답 DTO
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueStatusResponse {

    private final String status;                    // 대기열 상태 (WAITING / ALLOWED / EXPIRED)
    private final Long queueNumber;                 // 내 순번 (1-based, ALLOWED/EXPIRED 시 null)
    private final Long remainingAhead;              // 내 앞에 남은 인원 수 (ALLOWED/EXPIRED 시 null)
    private final Long estimatedWaitSeconds;        // 예상 대기 시간 (초, ALLOWED/EXPIRED 시 null)
    private final LocalDateTime enteredAt;          // 대기열 진입 시각 (모든 상태 공통)
    private final LocalDateTime allowedAt;          // ALLOWED 로 승격된 시각 (WAITING 시 null)
    private final LocalDateTime entryDeadline;      // 좌석 점유 데드라인 (= allowedAt + sessionSeconds, WAITING 시 null)
    private final boolean soldOut;                  // 매진 여부 - true 면 프론트가 매진 안내

    /**
     * WAITING 응답 — 순번 정보 채우고 ALLOWED 관련은 null
     */
    public static QueueStatusResponse waiting(
            long queueNumber,
            long remainingAhead,
            long estimatedWaitSeconds,
            LocalDateTime enteredAt,
            boolean soldOut
    ) {
        return new QueueStatusResponse(
                "WAITING",
                queueNumber, remainingAhead, estimatedWaitSeconds,
                enteredAt, null, null, soldOut
        );
    }

    /** ALLOWED 응답 — 승격 시각 / 만료 시각 채우고 순번 관련은 null */
    public static QueueStatusResponse allowed(
            LocalDateTime enteredAt,
            LocalDateTime allowedAt,
            LocalDateTime entryDeadline
    ) {
        return new QueueStatusResponse(
                "ALLOWED",
                null, null, null,
                enteredAt, allowedAt, entryDeadline, false
        );
    }

    /** EXPIRED 응답 — 진입 시각만, 나머지 null */
    public static QueueStatusResponse expired(LocalDateTime enteredAt) {
        return new QueueStatusResponse(
                "EXPIRED",
                null, null, null,
                enteredAt, null, null, false
        );
    }
}
