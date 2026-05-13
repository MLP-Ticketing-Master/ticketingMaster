package com.ticketmaster.backend.domain.queue.repository;

import com.ticketmaster.backend.domain.queue.entity.Queue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Queue 엔티티의 DB 작업 담당
 */
public interface QueueRepository extends JpaRepository<Queue, Long> {

    /** 발급된 토큰 문자열로 이력 레코드 찾기 (테스트 검증용) */
    Optional<Queue> findByQueueToken(String queueToken);
}
