package com.ticketmaster.backend.domain.queue.util;

import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 입장 권한 검증 유틸 — 큐 통과한 토큰만 좌석/결제 API 진입 허용
 * <p>
 * 현재 호출처: SeatController.reserve()
 */
@Component
@RequiredArgsConstructor
public class QueueTokenValidator {

    private final QueueRedisRepository queueRedis;

    // 부하테스트 시나리오 D 용 - false 시 검증 스킵 (대기열 off 효과)
    @Value("${queue.enabled:true}")
    private boolean queueEnabled;

    /**
     * matchId 의 대기열에서 token 이 ALLOWED 상태가 아니면 예외
     */
    public void validateAllowed(Long matchId, String token) {
        // 대기열 off 모드 - 부하테스트 비교용
        if (!queueEnabled) {
            return;
        }
        // 헤더 누락 / 빈 문자열 → 예외
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.QUEUE_TOKEN_NOT_FOUND);
        }
        // 헤더는 있는데 ALLOWED 아님 (만료 / 위조 등) → 예외
        if (!queueRedis.isAllowed(matchId, token)) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_PASSED);
        }
    }
}
