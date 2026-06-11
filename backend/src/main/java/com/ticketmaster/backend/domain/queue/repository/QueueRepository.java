package com.ticketmaster.backend.domain.queue.repository;

import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.entity.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Queue 엔티티의 DB 작업 담당
 */
public interface QueueRepository extends JpaRepository<Queue, Long> {

    /** 발급된 토큰 문자열로 이력 레코드 찾기 (테스트 검증용) */
    Optional<Queue> findByQueueToken(String queueToken);

    /**
     * 여러 토큰의 이력을 한 번에 조회 (스케줄러가 DB status 일괄 갱신할 때 사용)
     */
    List<Queue> findByQueueTokenIn(List<String> queueTokens);

    /**
     * 같은 user × match 조합의 활성(아직 EXPIRED 아닌) Queue 이력 조회
     * 결제 완료 시 admission 회수 — 해당 이력 EXPIRED 로 일괄 전환에 사용
     */
    List<Queue> findByUser_IdAndMatch_IdAndStatusNot(Long userId, Long matchId, QueueStatus status);
}
