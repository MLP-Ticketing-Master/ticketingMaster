package com.ticketmaster.backend.domain.seat.repository;

import com.ticketmaster.backend.domain.seat.entity.Seat;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** 회차 내 좌석 코드 중복 체크 — 단건 등록 시 */
    boolean existsByMatchIdAndSeatCode(Long matchId, String seatCode);

    /**
     * 일괄 등록 시 이미 존재하는 좌석 코드만 골라서 반환 - IN절 한 번 쿼리로 N+1 방지
     * Collection<String> -> 문자열들의 모음, seatCode의 모음
     */
    @Query("SELECT s.seatCode FROM Seat s WHERE s.match.id = :matchId AND s.seatCode IN :codes")
    List<String> findExistingSeatCodes(@Param("matchId") Long matchId,
                                       @Param("codes") Collection<String> codes);

    /**
     * 회차 좌석 전체 조회 — section/seatGrade 까지 fetch join 으로 한 번에 로딩
     */
    @Query("""
           SELECT s FROM Seat s
           JOIN FETCH s.section
           JOIN FETCH s.seatGrade
           WHERE s.match.id = :matchId
           ORDER BY s.rowLabel ASC, s.seatNo ASC
           """)
    List<Seat> findAllWithSectionAndGradeByMatchId(@Param("matchId") Long matchId);

    /** SEAT_GRADE_IN_USE / SECTION_IN_USE 검증용 */
    boolean existsBySeatGradeId(Long seatGradeId);
    boolean existsBySectionId(Long sectionId);
}
