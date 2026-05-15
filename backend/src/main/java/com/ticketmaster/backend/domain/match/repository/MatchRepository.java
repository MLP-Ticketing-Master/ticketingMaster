package com.ticketmaster.backend.domain.match.repository;

import com.ticketmaster.backend.domain.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Page<Match> findByEventId(Long eventId, Pageable pageable);

    /**
     * 대기열 승격 스케줄러용 — 현재 시각 기준 예매 가능 상태인 매치 조회
     * (Event.bookingOpenAt ≤ now ≤ Event.bookingCloseAt)
     */
    @Query("""
           SELECT m FROM Match m JOIN m.event e
           WHERE e.bookingOpenAt <= :now AND e.bookingCloseAt >= :now
           """)
    List<Match> findActiveMatchesForQueueAdmission(@Param("now") LocalDateTime now);

}
