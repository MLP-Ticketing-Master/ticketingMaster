package com.ticketmaster.backend.domain.seat.entity;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_section_event_name", columnNames = {"event_id", "name"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(length = 200)
    private String description;

    // ==================================================
    // 사용자 기능
    // ==================================================
    // (현재 정의된 메서드 없음 — 추후 추가)


    // ==================================================
    // 관리자 기능
    // ==================================================

    /** 구역 생성
     * — admin/section Service에서 호출
     * */
    public static Section create(Event event, String name, int displayOrder, String description) {
        Section s = new Section();
        s.event = event;
        s.name = name;
        s.displayOrder = displayOrder;
        s.description = description;
        return s;
    }

    /**
     * 구역 부분 수정 (PATCH)
     * - admin/section Service에서 호출
     */
    public void update(String name, Integer displayOrder, String description) {
        if (name != null) this.name = name;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (description != null) this.description = description;
    }
}