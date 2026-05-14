package com.ticketmaster.backend.domain.queue.service;

import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 한 매치의 대기열 상위 N명을 WAITING → ALLOWED 로 승격하는 트랜잭션 단위
 * 스케줄러(QueueAdmissionScheduler)가 30초마다 활성 매치별로 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueAdmissionService {

    /** 한 번에 승격할 인원 — application.yaml 의 queue.admission-batch-size */
    @Value("${queue.admission-batch-size}")
    private int admissionBatchSize;

    /** ALLOWED 토큰 TTL(초) — application.yaml 의 queue.session-seconds */
    @Value("${queue.session-seconds}")
    private int sessionSeconds;

    private final QueueRepository queueRepository;
    private final QueueRedisRepository queueRedis;

    /**
     * Redis 의 promoteTopN 자체는 트랜잭션 보장 X
     * DB Queue 테이블은 모니터링 / 감사 추적용 — 대기열 상태는 Redis를 기준으로 관리
     * DB 갱신이 실패해도 입장 권한은 Redis 기준으로 작동함 (정합성 자동 보정은 안 됨)
     */
    @Transactional
    public void promoteForMatch(Long matchId, LocalDateTime now) {
        // 1) Redis 에서 상위 N명 승격 처리 → 승격된 토큰 목록 받음
        List<String> promoted = queueRedis.promoteTopN(matchId, admissionBatchSize, sessionSeconds);
        if (promoted.isEmpty()) {
            return;
        }

        try {
            // 2) DB 의 해당 Queue 행들도 ALLOWED 로 갱신 (이력용)
            List<Queue> queues = queueRepository.findByQueueTokenIn(promoted);
            for (Queue q : queues) {
                q.markAllowed(now);
            }
            log.info("[QueueAdmission] matchId={} promoted={}", matchId, promoted.size());
        } catch (Exception e) {
            // DB 갱신 실패 — Redis 는 ALLOWED, DB 는 WAITING 으로 어긋남
            // 사용자 권한엔 영향 없으나 수동 보정용으로 토큰 목록 남김
            log.error("[QueueAdmission] DB 동기화 실패! matchId={}, 어긋난 토큰 수={}, tokens={}",
                    matchId, promoted.size(), promoted, e);
            throw e;  // 트랜잭션 롤백 위해 재던짐
        }
    }
}
