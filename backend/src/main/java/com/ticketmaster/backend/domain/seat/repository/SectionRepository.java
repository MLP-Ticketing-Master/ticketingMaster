package com.ticketmaster.backend.domain.seat.repository;

import com.ticketmaster.backend.domain.seat.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    /** 대회별 구역 목록 — 와이어프레임 "구역 관리" 카드 노출용 */
    List<Section> findAllByEventIdOrderByDisplayOrderAsc(Long eventId);

    /** 같은 이벤트 내에 동일한 이름의 구역이 존재하는지 — 등록/수정 시 중복 체크용 */
    boolean existsByEventIdAndName(Long eventId, String name);

    /** 같은 이벤트 내에 동일한 displayOrder의 구역이 존재하는지 — 등록/수정 시 슬롯 충돌 체크용 */
    boolean existsByEventIdAndDisplayOrder(Long eventId, Integer displayOrder);
}
