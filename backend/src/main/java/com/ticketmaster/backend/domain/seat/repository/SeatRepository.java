package com.ticketmaster.backend.domain.seat.repository;

import com.ticketmaster.backend.domain.seat.entity.Seat;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // -------------------------------------------------------
    // 관리자 기능
    // -------------------------------------------------------

    /** 회차 내 동일 구역에 같은 좌석 코드가 있는지 검사 — 단건 등록 시 */
    boolean existsByMatchIdAndSectionIdAndSeatCode(Long matchId, Long sectionId, String seatCode);

    /**
     * 일괄 등록 시 매치 안에서 이미 존재하는 (구역, 좌석 코드) 페어 반환 - IN절 한 번 쿼리로 N+1 방지
     * 같은 코드라도 구역이 다르면 허용되므로 코드만으로 검사 불가 — 호출부에서 (sectionId, code) 페어로 충돌 확인 필요
     * 반환: Object[]{sectionId(Long), seatCode(String)}
     */
    @Query("SELECT s.section.id, s.seatCode FROM Seat s WHERE s.match.id = :matchId AND s.seatCode IN :codes")
    List<Object[]> findExistingSectionCodePairs(@Param("matchId") Long matchId,
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

    // -------------------------------------------------------
    // 좌석 조회 (사용자 기능)
    // -------------------------------------------------------

    /**
     * 1단계 잔여 카운트용 — 매치 좌석의 ID/구역ID/등급ID/상태만 조회
     * Entity 전체 로딩 대신 필요 컬럼만 가져와 메모리 집계 (N+1 방지)
     * <p>
     * 반환: Object[]{seatId, sectionId, gradeId, status}
     */
    @Query("""
           SELECT s.id, s.section.id, s.seatGrade.id, s.status
           FROM Seat s
           WHERE s.match.id = :matchId
           """)
    List<Object[]> findIdAndGroupingByMatchId(@Param("matchId") Long matchId);

    /**
     * 2단계 구역 내 좌석 조회 — SeatGrade fetch join 으로 등급 정보 함께 로딩
     */
    @Query("""
           SELECT s FROM Seat s
           JOIN FETCH s.seatGrade
           WHERE s.match.id = :matchId AND s.section.id = :sectionId
           ORDER BY s.rowLabel ASC, s.seatNo ASC
           """)
    List<Seat> findBySectionAndMatch(@Param("matchId") Long matchId,
                                     @Param("sectionId") Long sectionId);

    /**
     * 특정 경기의 좌석들을 ID로 일괄 조회 (좌석 점유/해제 처리 시 사용)
     * SeatGrade를 fetch join하여 totalPrice 계산 시 N+1 방지
     */
    @Query("""
       SELECT s FROM Seat s
       JOIN FETCH s.seatGrade
       WHERE s.match.id = :matchId AND s.id IN :seatIds
       """)
    List<Seat> findByMatchAndIdIn(@Param("matchId") Long matchId,
                                  @Param("seatIds") Collection<Long> seatIds);


    /**
     * 만료 시간이 지난 점유 좌석을 찾아 반환
     *
     * - 조건: 현재 RESERVED 상태이면서, 점유 만료 시각이 지금보다 과거인 좌석
     * - SOLD(판매 완료) 좌석은 조건에 안 걸려서 자동 제외
     * - 사용자가 본인 해제한 좌석도 이미 AVAILABLE 이라 제외
     */
    @Query("""
           SELECT s FROM Seat s
           WHERE s.status = 'RESERVED'
           AND s.reservedUntil < :now
           """)
    List<Seat> findExpiredReservedSeats(@Param("now") LocalDateTime now);
}
