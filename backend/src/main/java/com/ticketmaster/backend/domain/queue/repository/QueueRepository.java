package com.ticketmaster.backend.domain.queue.repository;

import com.ticketmaster.backend.domain.queue.entity.Queue;
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
}
