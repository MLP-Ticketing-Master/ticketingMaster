package com.ticketmaster.backend.domain.seat.entity;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "seat_grades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** VIP / R / S / A 등 */
    @Column(name = "grade_code", nullable = false, length = 20)
    private String gradeCode;

    @Column(nullable = false)
    private int price;

    @Column(name = "color_hex", length = 10)
    private String colorHex;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** 좌석 등급 가격 변경 (관리자 기능) */
    public void changePrice(int newPrice) {
        // TODO: 음수/0 검증은 Service 또는 여기서 직접
        this.price = newPrice;
    }
}
