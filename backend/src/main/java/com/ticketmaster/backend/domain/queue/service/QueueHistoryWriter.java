package com.ticketmaster.backend.domain.queue.service;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 진입 이력 실제 저장 담당
// 버퍼/스케줄러(QueueHistoryService)와 분리한 이유: 같은 빈 안에서 호출하면
// 프록시를 거치지 않아 @Transactional 이 안 먹음 → 별도 빈으로 분리
@Service
@RequiredArgsConstructor
public class QueueHistoryWriter {

    private final QueueRepository queueRepository;

    @PersistenceContext
    private EntityManager em;

    // 한 트랜잭션 안에서 100건씩 모아 한 번에 보냄(batch_size=100)
    // getReference: user/match 를 다시 조회(SELECT)하지 않고 FK 만 채워 넣음
    @Transactional
    public void saveBatch(List<QueueHistoryRecord> batch) {
        List<Queue> entities = new ArrayList<>(batch.size());
        for (QueueHistoryRecord r : batch) {
            User userRef = em.getReference(User.class, r.userId());
            Match matchRef = em.getReference(Match.class, r.matchId());
            Queue queue = Queue.createWaiting(
                    userRef, matchRef, r.token(), r.queueNumber(), r.enteredAt(), r.expiresAt());
            if (r.allowed()) {
                queue.markAllowed(r.enteredAt());   // 즉시승격이면 ALLOWED 로 기록
            }
            entities.add(queue);
        }
        queueRepository.saveAll(entities);
    }
}
