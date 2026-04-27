package com.ticketmaster.backend.domain.seat.repository;

import com.ticketmaster.backend.domain.seat.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    /** 대회별 구역 목록 — 와이어프레임 "구역 관리" 카드 노출용 */
    List<Section> findAllByEventIdOrderByDisplayOrderAsc(Long eventId);
}
