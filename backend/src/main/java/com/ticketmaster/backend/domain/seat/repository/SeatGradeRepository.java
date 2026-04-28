package com.ticketmaster.backend.domain.seat.repository;

import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {

    /** 같은 대회 내 등급 코드 중복 체크 (POST 시) — 활성 데이터만 */
    boolean existsByEventIdAndGradeCode(Long eventId, String gradeCode);

    /** 대회별 등급 목록 — 가격 내림차순 (VIP가 가장 위) */
    List<SeatGrade> findAllByEventIdOrderByPriceDesc(Long eventId);

    /**
     * 삭제된 행까지 포함해서 조회 (재등록 시 복구용)
     * - @SQLRestriction 우회를 위해 native query 사용
     */
    @Query(value = "SELECT * FROM seat_grades " +
            "WHERE event_id = :eventId AND grade_code = :gradeCode",
            nativeQuery = true)
    Optional<SeatGrade> findByEventIdAndGradeCodeIncludingDeleted(
            @Param("eventId") Long eventId,
            @Param("gradeCode") String gradeCode);
}
