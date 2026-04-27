package com.ticketmaster.backend.domain.seat.entity;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.global.common.BaseEntity;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회차 단위 좌석
 * 상태 전이: AVAILABLE → RESERVED → SOLD (또는 RESERVED → AVAILABLE)
 */
@Getter
@Entity
@Table(
        name = "seats",
        indexes = {
                @Index(name = "idx_seat_match_status", columnList = "match_id,status"),
                @Index(name = "idx_seat_match_section", columnList = "match_id,section_id"),
                @Index(name = "idx_seat_match_grade",   columnList = "match_id,seat_grade_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_grade_id", nullable = false)
    private SeatGrade seatGrade;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_no", nullable = false)
    private int seatNo;

    @Column(name = "seat_code", nullable = false, length = 50)
    private String seatCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    /** RESERVED 상태일 때만 의미 있음 */
    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    // 상태 변경 메서드
    /** AVAILABLE → RESERVED */
    public void reserve(LocalDateTime until) {
        if (this.status == SeatStatus.SOLD) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_SOLD);
        }
        if (this.status == SeatStatus.RESERVED) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED);
        }
        this.status = SeatStatus.RESERVED;
        this.reservedUntil = until;
    }

    /** RESERVED → AVAILABLE */
    public void release() {
        if (this.status != SeatStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_SEAT_STATUS);
        }
        this.status = SeatStatus.AVAILABLE;
        this.reservedUntil = null;
    }

    /** RESERVED → SOLD (결제 성공 시) */
    public void markAsSold() {
        if (this.status != SeatStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_SEAT_STATUS);
        }
        this.status = SeatStatus.SOLD;
        this.reservedUntil = null;
    }

    /** SOLD → AVAILABLE (관리자 강제 취소 / 예매 취소 시) */
    public void restoreFromSold() {
        if (this.status != SeatStatus.SOLD) {
            throw new BusinessException(ErrorCode.INVALID_SEAT_STATUS);
        }
        this.status = SeatStatus.AVAILABLE;
    }
}
