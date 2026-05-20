package com.ticketmaster.backend.domain.queue.repository;


import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

/**
 * 대기열의 Redis 처리 전용 클래스
 * <p>
 * 1) queue:match:{matchId}              [Sorted Set]
 * 회차별 대기 명단(WAITING), member = 토큰, score = 진입 시각(ms)
 * score 오름차순으로 정렬되므로 ZRANK + 1 = 사용자 순번
 * → "내가 몇 번째인가" 조회에 사용
 * <p>
 * 2) queue:token:{token}                [Hash]
 * 토큰별 메타 정보, 필드 = userId, matchId, enteredAt, status, allowedAt
 * status = WAITING 또는 ALLOWED, allowedAt 은 승격 시 채워짐
 * → "이 토큰의 소유자와 상태"를 조회하는 용도
 * <p>
 * 3) queue:user:{userId}:{matchId}      [String]
 * 사용자별 회차 진입 마커, value = 발급된 토큰
 * → 중복 진입 방지용, SETNX로 원자적 검사+저장을 수행해서
 * 동시 요청이 와도 한 사용자당 하나의 토큰만 발급되도록 보장
 * <p>
 * 4) queue:allowed:{matchId}:{token}    [String]
 * ALLOWED 권한 표시, value = "1"
 * 스케줄러가 승격 시 토큰별로 생성, 키에 matchId 를 포함시켜
 * EXISTS 한 번으로 회차 검증 + 권한 검증 + TTL 만료 동시에 처리
 * → 좌석 API 같은 보호 대상 권한 검증에 사용
 * <p>
 * TTL
 * - WAITING 단계 키 (1)(2)(3): token-ttl-seconds (30분)
 * 좌석 진입까지 충분한 시간 + 비정상 종료 시 자동 정리
 * - ALLOWED 단계 키 (4): session-seconds (10분)
 * 좌석 페이지 머무름 + 점유 클릭까지의 데드라인
 * 키별 개별 TTL 이라 토큰마다 정확히 만료
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

    /**
     * Hash 에 저장된 토큰 메타 정보 조회
     * <p>
     * 반환 예: { userId, matchId, enteredAt, status, allowedAt }
     * Redis 에 키 자체가 없으면 빈 Map 반환 (호출하는 쪽에서 만료 판단)
     */
    public Map<String, String> getTokenMeta(String token) {
        Map<Object, Object> entries = redis.opsForHash().entries(tokenKey(token));
        Map<String, String> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
        return result;
    }

    /**
     * 토큰이 ALLOWED 상태인지 확인
     * queue:allowed:{matchId}:{token} 키 존재 여부로 판정
     * - 회차 검증 + 권한 검증 + TTL 만료 체크가 EXISTS 한 번에 끝남
     * - 개별 TTL 만료 시 키 자동 삭제 = 권한 자동 회수
     */
    public boolean isAllowed(Long matchId, String token) {
        Boolean exists = redis.hasKey(allowedKey(matchId, token));
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 토큰이 WAITING 상태인지 확인
     * queue:match:{matchId} Sorted Set 에 멤버로 있으면 ZRANK 가 not null
     */
    public boolean isWaiting(Long matchId, String token) {
        Long rank = redis.opsForZSet().rank(matchKey(matchId), token);
        return rank != null;
    }

    /**
     * 토큰의 현재 순번 (1-based)
     * ZRANK 가 0-based 라 +1 해서 반환
     */
    public long getRank(Long matchId, String token) {
        Long rank = redis.opsForZSet().rank(matchKey(matchId), token);
        return (rank == null ? 0L : rank) + 1L;
    }

    /**
     * 상위 n명을 WAITING → ALLOWED 로 승격
     * <p>
     * 동작 순서
     * 1) Sorted Set 에서 ZPOPMIN 으로 상위 n명 토큰 추출 (대기 명단에서 빠짐)
     * 2) 토큰별 queue:allowed:{matchId}:{token} 키 생성 (TTL = sessionSeconds, 개별 만료)
     * 3) 토큰 Hash 의 status 를 ALLOWED 로, allowedAt 을 현재 시각(ms) 으로 갱신
     *
     * @return 승격된 토큰 목록 (대기열이 비어있으면 빈 리스트)
     */
    public List<String> promoteTopN(Long matchId, int n, int sessionSeconds) {
        Duration sessionTtl = Duration.ofSeconds(sessionSeconds);

        // 1) 상위 n명 추출 (ZPOPMIN — 빼면서 동시에 정렬 기준 작은 것부터 가져옴)
        Set<ZSetOperations.TypedTuple<String>> popped =
                redis.opsForZSet().popMin(matchKey(matchId), n);
        if (popped == null || popped.isEmpty()) {
            return List.of();
        }

        List<String> tokens = popped.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .toList();

        // 2) 토큰별 allowed 키 생성 + Hash status/allowedAt 갱신
        String nowMs = String.valueOf(System.currentTimeMillis());
        for (String token : tokens) {
            redis.opsForValue().set(allowedKey(matchId, token), "1", sessionTtl);
            Map<String, String> updates = new HashMap<>();
            updates.put("status", "ALLOWED");
            updates.put("allowedAt", nowMs);
            redis.opsForHash().putAll(tokenKey(token), updates);
        }

        return tokens;
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

    private String allowedKey(Long matchId, String token) {
        return "queue:allowed:" + matchId + ":" + token;
    }
}
