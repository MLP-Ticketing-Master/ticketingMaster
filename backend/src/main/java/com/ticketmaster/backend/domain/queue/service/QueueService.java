package com.ticketmaster.backend.domain.queue.service;

import com.ticketmaster.backend.domain.match.dto.MatchBookingGate;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.match.service.MatchQueryService;
import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.dto.response.QueueStatusResponse;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.entity.QueueStatus;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 대기열 진입 / 권한 회수 서비스
 * <p>
 * 역할 분담
 * - Redis 동작 : QueueRedisRepository 에 위임
 * - DB  동작 : QueueRepository 에 위임
 * - 이 서비스 : 진입 흐름 (검증 → 토큰 발급 → Redis 등록 → DB 이력 저장) + 결제 완료 시 admission 회수
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

    /**
     * burst 게이트 ON/OFF — application.yaml 의 queue.burst-enabled
     * 운영 = true, 시연/로컬 = false
     */
    @Value("${queue.burst-enabled}")
    private boolean burstEnabled;

    private final MatchRepository matchRepository;
    private final QueueRepository queueRepository;
    private final QueueRedisRepository queueRedis;
    private final MatchQueryService matchQueryService;
    private final QueueHistoryService queueHistoryService;

    /**
     * 대기열 진입
     *
     * <p>
     * 응답 경로에서 동기 DB 호출을 0으로 둔 hot-path - 검증은 캐시, 등록은 Redis,
     * 이력은 비동기로 분리해 DB 커넥션 점유 없이 응답함
     *
     * <p>
     * 흐름
     * 1) 예매 가능 검증 — 캐시된 게이트만 확인 (적중 시 DB 0, 미스만 DB 1회)
     * 2) 토큰 발급 + Redis 등록 — 재진입(같은 userId)이면 기존 토큰을 그대로 받아옴 (멱등)
     * 3) 신규 진입일 때만 DB 이력 비동기 저장 (응답/커넥션 풀 점유와 분리)
     * 4) 응답 조립 — Redis 결과만으로 (순번 / 남은 인원 / 예상 대기 시간)
     */
    public QueueEnterResponse enter(Long matchId, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 예매 가능 검증 - 캐시된 게이트만 확인 (적중 시 DB 0, 미스만 DB 1회)
        MatchBookingGate gate = matchQueryService.getBookingGate(matchId);
        if (!gate.isBookableAt(now)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_OPEN);
        }

        // 2) 토큰 발급 + Redis 등록 — hot-path 의 유일한 I/O
        //    재진입이면 newToken 무시되고 기존 토큰이 그대로 돌아옴 (멱등성)
        String newToken = UUID.randomUUID().toString();
        long enteredAtMs = System.currentTimeMillis();

        QueueRedisRepository.EnterResult result =
                queueRedis.enter(matchId, userId, newToken, enteredAtMs,
                        admissionBatchSize, sessionSeconds, burstEnabled);
        String token = result.token();
        long queueNumber = result.rank();

        // 3) DB 이력 저장 — 신규 진입일 때만 비동기 디스패치 (응답/풀 점유와 분리)
        if (token.equals(newToken)) {
            LocalDateTime expiresAt = now.plusSeconds(tokenTtlSeconds); // 토큰 만료 시각 계산
            queueHistoryService.saveWaitingHistoryAsync(
                    userId, matchId, token, queueNumber, now, expiresAt, result.allowed());
        }

        // 4) 응답 조립 — Redis 결과만으로 (DB 무관)
        if (result.allowed()) {
            LocalDateTime entryDeadline = now.plusSeconds(sessionSeconds);
            log.info("[Queue] burst 즉시승격 matchId={} userId={} token={}", matchId, userId, token);
            return QueueEnterResponse.allowed(token, now, now, entryDeadline);
        }

        // 그 외 — WAITING 응답
        //    remainingAhead = 내 앞에 남은 사람 수 (음수가 안 되도록 max)
        long remainingAhead = Math.max(0L, queueNumber - 1L);
        //    예상 대기 시간 = (내 앞 사람 수 / 한 번에 처리하는 인원) * 처리 주기
        long estimatedWaitSeconds =
                (remainingAhead / admissionBatchSize) * admissionIntervalSeconds;

        log.info("[Queue] 진입 matchId={} userId={} token={} number={}",
                matchId, userId, token, queueNumber);

        return QueueEnterResponse.waiting(token, queueNumber, remainingAhead, estimatedWaitSeconds, now);
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
            // 매진 여부는 Redis 플래그로 확인 (DB COUNT 안 침 — 폴링 병목 회피)
            boolean soldOut = queueRedis.isSoldOut(matchId);
            return QueueStatusResponse.waiting(queueNumber, remainingAhead, estimatedWaitSeconds, enteredAt, soldOut);
        }

        // 5-3) Sorted Set 에서 빠졌고 allowed 키도 만료 → 권한 만료
        //      (Hash 는 30분 TTL 이라 아직 살아있어서 여기까지 도달)
        return QueueStatusResponse.expired(enteredAt);
    }

    /**
     * 결제 완료 시 사용자의 admission 회수
     * <p>
     * 원칙: 결제 완료 = 1회 예매 종료. 다음 예매는 다시 대기열부터 → maxTickets 모순 제거 + 공정성 강화
     * <p>
     * 동작
     * - Redis: admission 권한 키 + 토큰 메타 + 사용자 마커 삭제 (즉시 권한 회수)
     * - DB: 해당 user × match 의 활성 Queue 이력을 EXPIRED 로 일괄 전환 (감사용)
     * <p>
     * 호출자: PaymentService.confirm() DB 트랜잭션 성공 직후
     * 호출자가 try/catch 로 감싸 Redis/DB 실패가 결제 자체에 영향 주지 않도록 처리
     */
    @Transactional
    public void expireUserAdmission(Long matchId, Long userId) {
        queueRedis.clearUserAdmission(matchId, userId);

        List<Queue> active = queueRepository.findByUser_IdAndMatch_IdAndStatusNot(
                userId, matchId, QueueStatus.EXPIRED);
        for (Queue q : active) {
            q.markExpired();
        }
        log.info("[Queue] admission 회수 matchId={} userId={} expired={}",
                matchId, userId, active.size());
    }
}
