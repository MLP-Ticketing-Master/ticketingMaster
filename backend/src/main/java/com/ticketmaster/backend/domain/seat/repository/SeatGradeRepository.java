package com.ticketmaster.backend.domain.seat.repository;

import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {

    /** 같은 대회 내 등급 코드 중복 체크 (POST 시) */
    boolean existsByEventIdAndGradeCode(Long eventId, String gradeCode);

    /** 대회별 등급 목록 — 가격 내림차순 (VIP가 가장 위) */
    List<SeatGrade> findAllByEventIdOrderByPriceDesc(Long eventId);
}
