package com.ticketmaster.backend.domain.seat.repository;

import com.ticketmaster.backend.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** 회차 내 좌석 코드 중복 체크 — 단건/일괄 등록 시 */
    boolean existsByMatchIdAndSeatCode(Long matchId, String seatCode);

    /** 회차 좌석 전체 — 와이어프레임 좌석 그리드 렌더링용 */
    List<Seat> findAllByMatchIdOrderByRowLabelAscSeatNoAsc(Long matchId);

    /** SEAT_GRADE_IN_USE / SECTION_IN_USE 검증용 */
    boolean existsBySeatGradeId(Long seatGradeId);
    boolean existsBySectionId(Long sectionId);
}
