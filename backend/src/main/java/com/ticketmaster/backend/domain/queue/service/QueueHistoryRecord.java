package com.ticketmaster.backend.domain.queue.service;

import java.time.LocalDateTime;

// 버퍼에 쌓는 진입 이력 1건
// enter 는 여러 스레드가 동시에 호출함
// JPA 엔티티는 DB 세션에 묶여 있어 여러 스레드가 공유하면 깨지므로
// 버퍼엔 숫자/문자 같은 순수 값만 담고, 실제 엔티티 생성은 트랜잭션마다 독립 세션을 받는 flush 안에서 처리함
public record QueueHistoryRecord(
        Long userId,
        Long matchId,
        String token,
        long queueNumber,
        LocalDateTime enteredAt,
        LocalDateTime expiresAt,
        boolean allowed
) {
}
