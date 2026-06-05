package com.ticketmaster.backend.domain.queue.service;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 대기열 진입 이력 비동기 저장
 * 큐 순서/권한의 기준은 Redis이고 DB 이력은 보존 기록
 * 응답 속도를 위해 insert를 별도 스레드로 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueHistoryService {

    private final QueueRepository queueRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * 진입 이력 저장 - queueExecutor 스레드 풀에서 비동기 실행
     * getReference: SELECT 없이 FK 프록시만 만들어 user/match 재조회 회피
     * 실패해도 사용자 진입(Redis)에는 영향 없음 - 로그만 남기고 삼킴
     */
    @Async("queueExecutor")
    @Transactional
    public void saveWaitingHistoryAsync(Long userId, Long matchId, String token,
                                        long queueNumber, LocalDateTime enteredAt,
                                        LocalDateTime expiresAt, boolean allowed) {
        try {
            User userRef = em.getReference(User.class, userId);
            Match matchRef = em.getReference(Match.class, matchId);

            Queue queue = Queue.createWaiting(userRef, matchRef, token, queueNumber, enteredAt, expiresAt);
            if (allowed) {
                queue.markAllowed(enteredAt);   // 즉시승격이면 ALLOWED 로 기록
            }
            queueRepository.save(queue);
        } catch (Exception e) {
            log.error("[Queue] 진입 이력 비동기 저장 실패 userId={} matchId={} token={}",
                    userId, matchId, token, e);
        }
    }
}
