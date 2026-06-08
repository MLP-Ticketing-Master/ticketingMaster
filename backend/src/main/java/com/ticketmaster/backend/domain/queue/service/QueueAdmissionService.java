package com.ticketmaster.backend.domain.queue.service;

import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 한 매치의 대기열 상위 N명을 WAITING → ALLOWED 로 승격하는 트랜잭션 단위 (N = min(admission-batch-size, 남은 좌석 + 버퍼))
 * 스케줄러(QueueAdmissionScheduler)가 설정 주기(기본 10초)마다 활성 매치별로 호출
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

    /**
     * 남은 좌석보다 더 입장시킬 버퍼 - 미구매자 대비, 클수록 빨리 팔리지만 헛입장이 늘어남
     */
    @Value("${queue.admission-buffer:10}")
    private int admissionBuffer;

    private final QueueRepository queueRepository;
    private final QueueRedisRepository queueRedis;
    private final SeatRepository seatRepository;

    /**
     * Redis 의 promoteTopN 자체는 트랜잭션 보장 X
     * DB Queue 테이블은 모니터링 / 감사 추적용 — 대기열 상태는 Redis를 기준으로 관리
     * DB 갱신이 실패해도 입장 권한은 Redis 기준으로 작동함 (정합성 자동 보정은 안 됨)
     */
    @Transactional
    public void promoteForMatch(Long matchId, LocalDateTime now) {
        // 좌석 기반 동적 입장 — 예매 가능 좌석 수만큼(+버퍼)만 승격
        long available = seatRepository.countAvailableByMatchId(matchId);

        // 매진(예매 가능 좌석 0) — 승격 중단 + 매진 플래그 ON
        // 취소/미결제 만료로 좌석이 다시 풀리면 다음 주기에 자동으로 재개됨
        if (available <= 0) {
            queueRedis.markSoldOut(matchId);
            return;
        }
        queueRedis.clearSoldOut(matchId); // 좌석 있음 → 매진 아님

        // 남은 좌석 + 버퍼만큼만 입장 (배치 상한 admissionBatchSize 로 캡)
        // 버퍼는 미구매자 대비분 - 좌석 못 잡은 사람은 reserve 에서 깔끔한 409
        int toPromote = (int) Math.min(admissionBatchSize, available + admissionBuffer);

        List<String> promoted = queueRedis.promoteTopN(matchId, toPromote, sessionSeconds);
        if (promoted.isEmpty()) {
            return;
        }

        try {
            // DB Queue 행도 ALLOWED 로 갱신 (이력용)
            List<Queue> queues = queueRepository.findByQueueTokenIn(promoted);
            for (Queue q : queues) {
                q.markAllowed(now);
            }
            log.info("[QueueAdmission] matchId={} promoted={} (available={})",
                    matchId, promoted.size(), available);
        } catch (Exception e) {
            // DB 갱신 실패 — Redis 는 ALLOWED, DB 는 WAITING 으로 어긋남
            // 사용자 권한엔 영향 없으나 수동 보정용으로 토큰 목록 남김
            log.error("[QueueAdmission] DB 동기화 실패! matchId={}, 어긋난 토큰 수={}, tokens={}",
                    matchId, promoted.size(), promoted, e);
            throw e;  // 트랜잭션 롤백 위해 재던짐
        }
    }
}
