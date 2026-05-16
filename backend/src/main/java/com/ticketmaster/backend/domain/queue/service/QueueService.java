package com.ticketmaster.backend.domain.queue.service;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.dto.response.QueueStatusResponse;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * 대기열 진입 서비스
 * <p>
 * 역할 분담
 * - Redis 동작 : QueueRedisRepository 에 위임
 * - DB  동작 : QueueRepository 에 위임
 * - 이 서비스 : 흐름 (검증 → 토큰 발급 → Redis 등록 → DB 이력 저장)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    /**
     * 한 번에 처리하는 입장 인원 — application.yaml 의 queue.admission-batch-size
     */
    @Value("${queue.admission-batch-size}")
    private int admissionBatchSize;

    /**
     * 입장 스케줄러 주기(초) — application.yaml 의 queue.admission-interval-seconds
     */
    @Value("${queue.admission-interval-seconds}")
    private int admissionIntervalSeconds;

    /**
     * 대기열 토큰 TTL(초) — application.yaml 의 queue.token-ttl-seconds
     */
    @Value("${queue.token-ttl-seconds}")
    private int tokenTtlSeconds;

    /**
     * ALLOWED 토큰 TTL(초) — application.yaml 의 queue.session-seconds
     */
    @Value("${queue.session-seconds}")
    private int sessionSeconds;

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final QueueRepository queueRepository;
    private final QueueRedisRepository queueRedis;

    /**
     * 대기열 진입
     *
     * <p>
     * 흐름
     * 1) 회차 존재 확인 — 없으면 404
     * 2) 예매 가능 시간인지 검증 — 오픈 전이면 400
     * 3) 사용자 엔티티 조회 — Queue 의 user 필드에 넣을 참조
     * 4) UUID 토큰 발급
     * 5) Redis 등록 — 중복 진입은 여기서 409
     * 6) DB 이력 저장
     * 7) 응답 조립 (순번 / 남은 인원 / 예상 대기 시간)
     */
    @Transactional
    public QueueEnterResponse enter(Long matchId, Long userId) {
        // 1) 회차 존재 확인
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 2) 예매 가능 시간 검증 — event.status OPEN + match.status SCHEDULED + bookingOpenAt ≤ now ≤ bookingCloseAt
        LocalDateTime now = LocalDateTime.now();
        if (!match.isBookableAt(now)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_OPEN);
        }

        // 3) 사용자 엔티티 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4) UUID 토큰 발급 — 거의 충돌 확률 0 인 무작위 문자열
        String token = UUID.randomUUID().toString();
        long enteredAtMs = System.currentTimeMillis();

        // 5) Redis 등록 — 중복 진입이면 여기서 QUEUE_ALREADY_ENTERED
        long queueNumber = queueRedis.enter(matchId, userId, token, enteredAtMs);

        // 6) DB 에 이력 저장 (모니터링 / 디버깅 / 감사 추적용)
        LocalDateTime expiresAt = now.plusSeconds(tokenTtlSeconds); // 토큰 만료 시각 계산
        Queue queue = Queue.createWaiting(user, match, token, queueNumber, now, expiresAt);
        queueRepository.save(queue);

        // 7) 응답 조립
        //    remainingAhead = 내 앞에 남은 사람 수 (음수가 안 되도록 max)
        long remainingAhead = Math.max(0L, queueNumber - 1L);
        //    예상 대기 시간 = (내 앞 사람 수 / 한 번에 처리하는 인원) * 처리 주기
        long estimatedWaitSeconds =
                (remainingAhead / admissionBatchSize) * admissionIntervalSeconds;

        log.info("[Queue] 진입 matchId={} userId={} token={} number={}",
                matchId, userId, token, queueNumber);

        return QueueEnterResponse.of(token, queueNumber, remainingAhead, estimatedWaitSeconds, now);
    }

    /**
     * 대기열 상태 조회 (폴링용 — 프론트가 3초 단위로 호출)
     * <p>
     * 흐름
     * 1) 회차 존재 검증 — 없으면 MATCH_NOT_FOUND
     * 2) 토큰 형식 검증 (null/빈 문자열이면 QUEUE_TOKEN_NOT_FOUND)
     * 3) Redis Hash 에서 토큰 메타 조회 — 없으면 TTL 만료로 간주 (QUEUE_TOKEN_EXPIRED)
     * 4) 토큰의 matchId 와 요청 matchId 일치 확인 — 불일치 시 QUEUE_TOKEN_MATCH_MISMATCH
     * 5) 상태 판정: ALLOWED → WAITING → EXPIRED 순서로 확인
     * 6) 상태별 응답 조립
     */
    @Transactional(readOnly = true)
    public QueueStatusResponse getStatus(Long matchId, String token) {

        // 1) 회차 존재 검증 — 잘못된 matchId 면 404
        if (!matchRepository.existsById(matchId)) {
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
        }

        // 2) 토큰 형식 검증 — 헤더 누락 / 빈 문자열
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.QUEUE_TOKEN_NOT_FOUND);
        }

        // 3) 토큰 메타 조회 — Redis 키 자체가 없으면 TTL 만료
        Map<String, String> meta = queueRedis.getTokenMeta(token);
        if (meta.isEmpty()) {
            throw new BusinessException(ErrorCode.QUEUE_TOKEN_EXPIRED);
        }

        // 4) 토큰의 matchId 와 요청 matchId 일치 확인
        long tokenMatchId = Long.parseLong(meta.get("matchId"));
        if (tokenMatchId != matchId) {
            throw new BusinessException(ErrorCode.QUEUE_TOKEN_MATCH_MISMATCH);
        }

        // 진입 시각 디코딩 — Hash 에는 시각이 long(밀리초) 문자열로 저장돼있어서
        // LocalDateTime 으로 변환. 아래 모든 분기에서 쓰이므로 한 번만 처리
        long enteredAtMs = Long.parseLong(meta.get("enteredAt"));
        LocalDateTime enteredAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(enteredAtMs), ZoneId.systemDefault());

        // 5-1) ALLOWED 인지 먼저 확인
        if (queueRedis.isAllowed(matchId, token)) {
            // Hash 의 allowedAt 사용 — 스케줄러가 승격 시 채워둠
            String allowedAtStr = meta.get("allowedAt");
            long allowedAtMs = allowedAtStr != null
                    ? Long.parseLong(allowedAtStr)
                    : System.currentTimeMillis();
            LocalDateTime allowedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(allowedAtMs), ZoneId.systemDefault());
            LocalDateTime entryDeadline = allowedAt.plusSeconds(sessionSeconds);
            return QueueStatusResponse.allowed(enteredAt, allowedAt, entryDeadline);
        }

        // 5-2) WAITING 인지 확인
        if (queueRedis.isWaiting(matchId, token)) {
            long queueNumber = queueRedis.getRank(matchId, token);
            long remainingAhead = Math.max(0L, queueNumber - 1L);
            long estimatedWaitSeconds =
                    (remainingAhead / admissionBatchSize) * admissionIntervalSeconds;
            return QueueStatusResponse.waiting(queueNumber, remainingAhead, estimatedWaitSeconds, enteredAt);
        }

        // 5-3) Sorted Set 에서 빠졌고 allowed 키도 만료 → 권한 만료
        //      (Hash 는 30분 TTL 이라 아직 살아있어서 여기까지 도달)
        return QueueStatusResponse.expired(enteredAt);
    }
}
