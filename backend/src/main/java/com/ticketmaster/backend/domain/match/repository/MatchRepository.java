package com.ticketmaster.backend.domain.match.repository;

import com.ticketmaster.backend.domain.match.dto.MatchBookingGate;
import com.ticketmaster.backend.domain.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Page<Match> findByEventId(Long eventId, Pageable pageable);

    /**
     * 대기열 승격 스케줄러용 — 현재 시각 기준 예매 가능 상태인 매치 조회
     * (bookingOpenAt ≤ now ≤ bookingCloseAt)
     */
    @Query("""
            SELECT m FROM Match m
            WHERE m.bookingOpenAt <= :now AND m.bookingCloseAt >= :now
            """)
    List<Match> findActiveMatchesForQueueAdmission(@Param("now") LocalDateTime now);

    /**
     * 진행 중(SCHEDULED/LIVE) 회차에 배정된 팀 존재 여부
     */
    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM Match m
            WHERE (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId)
              AND m.status IN ('SCHEDULED', 'LIVE')
            """)
    boolean existsByTeamIdAndStatusInProgress(@Param("teamId") Long teamId);

    /**
     * 예매 게이트 값만 한 방에 조회
     * 엔티티 전체 로딩 대신 필요한 4개 필드만 가져옴
     */
    @Query("""
            SELECT new com.ticketmaster.backend.domain.match.dto.MatchBookingGate(
            m.event.status, m.status, m.bookingOpenAt, m.bookingCloseAt)
            FROM Match m
            WHERE m.id = :matchId
            """)
    Optional<MatchBookingGate> findBookingGate(@Param("matchId") Long matchId);
}
