package com.ticketmaster.backend.domain.queue.repository;


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
 * → 동일 사용자가 enter() 를 다시 호출하면 이 마커를 보고 상태에 따라 idempotent 하게 처리
 * (정상 세션 = WAITING/ALLOWED 살아있음 → 기존 토큰 재사용,
 *  좀비 마커 = ALLOWED 권한 만료된 잔재 → 정리 후 새 토큰으로 다시 줄서기)
 * 새로고침 / 창 닫기 / 시크릿창 같은 클라이언트 측 토큰 손실에도 같은 자리로 복귀시키기 위함
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
 * <p>
 * 조기 회수
 * - 결제 완료 시 clearUserAdmission() 으로 사용자별 키 일괄 삭제
 *   PaymentService.confirm() 성공 직후 호출 — 다음 예매는 다시 대기열부터 진입
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
     * enter() 결과 — 발급(또는 재사용)된 토큰과 현재 순번을 함께 반환
     * <p>
     * - 신규 진입       : token 은 호출자가 넘긴 값 그대로, rank 는 ZSet 의 새 순번
     * - WAITING 재진입  : token 은 기존에 발급된 값, rank 는 그 토큰의 현재 순번 (자기 자리 유지)
     * - ALLOWED 재진입  : token 은 기존에 발급된 값, rank 는 0 (ZSet 에서 이미 빠진 상태 — 정상 동작)
     * 호출 측은 status API 로 ALLOWED 확인 후 좌석 페이지로 복귀
     * - 권한 만료 재진입: token 은 새로 발급된 값, rank 는 ZSet 의 새 순번 (좀비 마커 정리 후 다시 줄서기)
     * - 신규 진입 시 burst 게이트 통과 여부도 함께 반환
     * - 재진입(idempotent) 케이스는 항상 false — 이미 자리 있음
     */
    public record EnterResult(String token, long rank, boolean allowed) {}

    /**
     * 대기열 진입을 Redis 에 등록
     * <p>
     * 동일 사용자가 이미 마커를 가지고 있으면 상태에 따라 분기 (idempotent)
     * - WAITING / ALLOWED 권한 살아있음 → 기존 토큰 그대로 반환 (새로고침 / 창 닫기 복구용)
     * - ALLOWED 권한 만료된 좀비 마커     → 잔재 정리 후 새 토큰 발급 (다시 줄서기)
     * <p>
     * 순서
     * 1) 사용자 인덱스 마커 SETNX
     * → 선점 성공 → 신규 진입 흐름 (ZSet + Hash 등록)
     * → 선점 실패 + 기존 토큰 유효 (WAITING/ALLOWED) → 그대로 재사용 반환 (ZSet/Hash 건드리지 않음)
     * → 선점 실패 + 기존 토큰 만료 (좀비)            → Hash 잔재 정리 후 새 토큰으로 신규 진입 흐름 진행
     * 2) Sorted Set 에 토큰 추가 (score 는 진입 시각 ms — 작을수록 앞 순번)
     * 3) Hash 에 토큰 메타 저장
     * 4) ZRANK + 1 로 순번 계산
     * 5) burst 게이트 시도 — burstEnabled=true 이고 신규 진입일 때만, INCR 결과 ≤ admissionBatchSize 면 즉시 ALLOWED
     *
     * @return 발급(또는 재사용)된 토큰 + 1-based 순번 + burst 게이트 통과 여부
     *         (재진입 케이스는 allowed 항상 false — 이미 자리 있어 카운터를 건드리지 않기 위함)
     */
    public EnterResult enter(Long matchId, Long userId, String token, long enteredAtMs,
                             int admissionBatchSize, int sessionSeconds,
                             boolean burstEnabled) {
        // application.yaml 에서 받은 초 단위 값을 Duration 으로 변환
        Duration tokenTtl = Duration.ofSeconds(tokenTtlSeconds);

        // 1) 사용자 인덱스 마커 선점 — SETNX 가 false 면 이미 발급된 토큰이 있다는 뜻
        //    setIfAbsent 는 Redis 의 SETNX 와 같음 ("키 없으면 생성하고 true, 있으면 false" 를 원자적으로 처리)
        String userKey = userIndexKey(userId, matchId);
        Boolean acquired = redis.opsForValue().setIfAbsent(userKey, token, tokenTtl);
        if (Boolean.FALSE.equals(acquired)) {
            // 기존 토큰 회수 후 상태에 따라 분기
            String existingToken = redis.opsForValue().get(userKey);
            if (existingToken != null) {
                // WAITING 또는 ALLOWED 권한이 살아있는 정상 세션 → 같은 토큰으로 복귀
                if (isWaiting(matchId, existingToken) || isAllowed(matchId, existingToken)) {
                    long existingRank = getRank(matchId, existingToken);
                    return new EnterResult(existingToken, existingRank, false);
                }
                // ALLOWED 권한이 만료된 좀비 마커 — ZSet/ALLOWED 둘 다 없고 마커+Hash 잔재만 남음
                // Hash 잔재까지 정리하고 아래 흐름에서 새 토큰으로 다시 줄서기
                redis.delete(tokenKey(existingToken));
            }
            // 새 토큰으로 마커 갱신 — 좀비 정리 직후 또는 SETNX 실패 직후 TTL 이 만료된 매우 드문 케이스
            redis.opsForValue().set(userKey, token, tokenTtl);
        }

        // SETNX 성공 — 신규 진입 메인 흐름
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
        long rankValue = (rank == null ? 0L : rank) + 1L;

        // 5) burst 게이트 시도 — burstEnabled=true 일 때만
        //    재진입은 위에서 빠져나가서 여긴 신규 진입만 도달함
        boolean burstAcquired = burstEnabled && tryAcquireBurst(matchId, admissionBatchSize);
        if (burstAcquired) {
            applyAllowed(matchId, token, sessionSeconds);
        }
        return new EnterResult(token, rankValue, burstAcquired);
    }

    // burst 슬롯 점유 시도
    // INCR 결과가 한도 이하면 점유 성공, 초과면 DECR로 롤백
    // 키 자체는 token-ttl-seconds 의 TTL로 자연 정리됨
    private boolean tryAcquireBurst(Long matchId, int limit) {
        String key = burstKey(matchId);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // 첫 INCR 직후에만 TTL 적용 — 이미 있는 키의 TTL을 갱신하지 않기 위함
            redis.expire(key, Duration.ofSeconds(tokenTtlSeconds));
        }
        if (count != null && count <= limit) {
            return true;
        }
        redis.opsForValue().decrement(key);
        return false;
    }

    // 단건 즉시승격 전용 — 배치는 promoteTopN 참고
    // ZSet 에서 제거(WAITING 명단에서 빠짐) + allowed 키 생성 + Hash status/allowedAt 갱신
    private void applyAllowed(Long matchId, String token, int sessionSeconds) {
        Duration sessionTtl = Duration.ofSeconds(sessionSeconds);
        redis.opsForZSet().remove(matchKey(matchId), token);
        redis.opsForValue().set(allowedKey(matchId, token), "1", sessionTtl);

        Map<String, String> updates = new HashMap<>();
        updates.put("status", "ALLOWED");
        updates.put("allowedAt", String.valueOf(System.currentTimeMillis()));
        String tokenKey = tokenKey(token);
        redis.opsForHash().putAll(tokenKey, updates);
        redis.expire(tokenKey, sessionTtl);
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
        //    Hash TTL 도 sessionTtl 로 줄여서 ALLOWED 키와 동시에 만료되도록 일치시킴
        //    (둘이 어긋나면 ALLOWED 살아있는데 Hash 만 만료되어 status 조회가 깨지는 엣지케이스 발생)
        String nowMs = String.valueOf(System.currentTimeMillis());
        for (String token : tokens) {
            redis.opsForValue().set(allowedKey(matchId, token), "1", sessionTtl);
            Map<String, String> updates = new HashMap<>();
            updates.put("status", "ALLOWED");
            updates.put("allowedAt", nowMs);
            String tokenKey = tokenKey(token);
            redis.opsForHash().putAll(tokenKey, updates);
            redis.expire(tokenKey, sessionTtl);
        }

        return tokens;
    }

    /**
     * 매진 플래그 ON - 예매 가능 좌석 0 일 때 스케줄러가 세팅 (TTL은 안전망)
     */
    public void markSoldOut(Long matchId) {
        redis.opsForValue().set(soldOutKey(matchId), "1", Duration.ofSeconds(tokenTtlSeconds));
    }

    /**
     * 매진 플래그 OFF - 좌석이 다시 풀리면 (취소/미결제 만료 등) 해제
     */
    public void clearSoldOut(Long matchId) {
        redis.delete(soldOutKey(matchId));
    }

    /**
     * 매진 여부 - 폴링 빈도 높은 getStatus 에서 DB 대신 Redis로 빠르게 확인하기 위함
     */
    public boolean isSoldOut(Long matchId) {
        return Boolean.TRUE.equals(redis.hasKey(soldOutKey(matchId)));
    }

    /**
     * 사용자의 admission 토큰 회수 — 결제 완료 후 호출
     * <p>
     * 동작
     * 1) 사용자 인덱스 마커로 토큰 역추적
     * 2) 토큰 관련 키 전부 삭제 (allowed / Hash / WAITING 잔재)
     * 3) 마지막에 마커 자체 삭제 → 같은 매치 재진입 시 새 줄서기 가능
     * <p>
     * 멱등 — 마커가 없거나 키 일부가 이미 만료돼도 안전하게 통과
     */
    public void clearUserAdmission(Long matchId, Long userId) {
        String userKey = userIndexKey(userId, matchId);
        String token = redis.opsForValue().get(userKey);
        if (token != null) {
            redis.delete(allowedKey(matchId, token));
            redis.delete(tokenKey(token));
            redis.opsForZSet().remove(matchKey(matchId), token);
        }
        redis.delete(userKey);
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

    private String burstKey(Long matchId) {
        return "queue:burst:" + matchId;
    }

    // queue:soldout:{matchId} — 매진 표시 플래그 (스케줄러가 좌석 잔여 기반으로 갱신)
    private String soldOutKey(Long matchId) {
        return "queue:soldout:" + matchId;
    }
}
