package com.ticketmaster.backend.domain.seat.entity;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "seat_grades",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seat_grade_event_code", columnNames = {"event_id", "grade_code"})
        })
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_grade_seq")
    @SequenceGenerator(name = "seat_grade_seq", sequenceName = "SEAT_GRADE_SEQ", allocationSize = 50)
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

    // ==================================================
    // 사용자 기능
    // ==================================================
    // (현재 정의된 메서드 없음 — 추후 추가)

    // ==================================================
    // 관리자 기능
    // ==================================================

    /**
     * 좌석 등급 생성
     * - admin/seatgrade Service에서 호출
     */
    public static SeatGrade create(Event event, String gradeCode,
                                   int price, String colorHex) {
        SeatGrade g = new SeatGrade();
        g.event = event;
        g.gradeCode = gradeCode;
        g.price = price;
        g.colorHex = colorHex;
        return g;
    }

    /**
     * 좌석 등급 부분 수정 (PATCH)
     * - null이 아닌 필드만 갱신
     * - admin/seatgrade Service에서 호출
     */
    public void update(Integer price, String colorHex) {
        if (price != null) {
            this.price = price;
        }
        if (colorHex != null) {
            this.colorHex = colorHex;
        }
    }
}
