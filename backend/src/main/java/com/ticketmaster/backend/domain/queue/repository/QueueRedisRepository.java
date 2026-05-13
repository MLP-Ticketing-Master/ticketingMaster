package com.ticketmaster.backend.domain.queue.repository;


import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 대기열의 Redis 처리 전용 클래스
 *
 * Redis 키 3개로 구성, 각 키의 역할이 다름
 * ({matchId}, {userId}, {token}은 런타임에 채워지는 식별자)
 *
 *  1) queue:match:{matchId}              [Sorted Set]
 *     회차별 대기 명단, member = 토큰, score = 진입 시각(ms)
 *     score 오름차순으로 정렬되므로 ZRANK + 1 = 사용자 순번
 *     → "내가 몇 번째인가" 조회에 사용
 *
 *  2) queue:token:{token}                [Hash]
 *     토큰별 메타 정보, 필드 = userId, matchId, enteredAt, status
 *     → "이 토큰의 소유자와 상태"를 조회하는 용도
 *
 *  3) queue:user:{userId}:{matchId}      [String]
 *     사용자별 회차 진입 마커, value = 발급된 토큰
 *     → 중복 진입 방지용, SETNX로 원자적 검사+저장을 수행해서
 *        동시 요청이 와도 한 사용자당 하나의 토큰만 발급되도록 보장
 *
 * 모든 키는 TTL 30분
 * (좌석 진입까지 충분한 시간 + 비정상 종료 시 자동 정리)
 */
@Repository
@RequiredArgsConstructor
public class QueueRedisRepository {

    private final StringRedisTemplate redis;

    /**
     * 대기열 토큰 TTL(초) — application.yaml 의 queue.token-ttl-seconds
     */
    @Value("${queue.token-ttl-seconds}")
    private int tokenTtlSeconds;

    /**
     * 대기열 진입을 Redis 에 등록
     * <p>
     * 순서
     * 1) "이 사용자가 이 회차에 이미 들어와 있냐" 인덱스 키를 SETNX (없을 때만 생성)
     * → 있으면 false 반환 → 중복 진입이라 예외
     * 2) Sorted Set 에 토큰 추가 (score 는 진입 시각 ms — 작을수록 앞 순번)
     * 3) Hash 에 토큰 메타 저장
     *
     * @return 1-based 순번 (ZRANK 가 0-based 라서 +1)
     */
    public long enter(Long matchId, Long userId, String token, long enteredAtMs) {
        // application.yaml 에서 받은 초 단위 값을 Duration 으로 변환
        Duration tokenTtl = Duration.ofSeconds(tokenTtlSeconds);

        // 1) 중복 진입 차단
        //    setIfAbsent 는 Redis 의 SETNX 와 같음: "키 없으면 생성하고 true, 있으면 false" 를 원자적으로 처리
        String userKey = userIndexKey(userId, matchId);
        Boolean acquired = redis.opsForValue().setIfAbsent(userKey, token, tokenTtl);
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException(ErrorCode.QUEUE_ALREADY_ENTERED);
        }

        // 2) Sorted Set 에 토큰 추가 (먼저 들어온 사람이 작은 score → 작은 순번)
        String matchKey = matchKey(matchId);
        redis.opsForZSet().add(matchKey, token, enteredAtMs);
        redis.expire(matchKey, tokenTtl);

        // 3) 토큰 메타 정보 Hash 저장
        Map<String, String> meta = new HashMap<>();
        meta.put("userId", String.valueOf(userId));
        meta.put("matchId", String.valueOf(matchId));
        meta.put("enteredAt", String.valueOf(enteredAtMs));
        meta.put("status", "WAITING");
        String tokenKey = tokenKey(token);
        redis.opsForHash().putAll(tokenKey, meta);
        redis.expire(tokenKey, tokenTtl);

        // 4) 순번 = ZRANK + 1
        Long rank = redis.opsForZSet().rank(matchKey, token);
        return (rank == null ? 0L : rank) + 1L;
    }

    // ─── 키 생성 헬퍼 (같은 키 형식을 한 곳에서 만들어서 실수 방지) ───

    private String matchKey(Long matchId) {
        return "queue:match:" + matchId;
    }

    private String tokenKey(String token) {
        return "queue:token:" + token;
    }

    private String userIndexKey(Long userId, Long matchId) {
        return "queue:user:" + userId + ":" + matchId;
    }
}
