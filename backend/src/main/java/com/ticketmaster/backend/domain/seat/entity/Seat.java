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
 *
 * 좌석 동시성 설계
 *  - 모든 상태(AVAILABLE/RESERVED/SOLD)를 DB로 관리
 *  - 점유: JPA @Version 낙관적 락으로 처리
 *  - 만료: 스케줄러가 30초 주기로 reservedUntil 경과 좌석 자동 복구
 */
@Getter
@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seat_match_code", columnNames = {"match_id", "seat_code"})
        },
        indexes = {
                @Index(name = "idx_seat_match_status", columnList = "match_id,status"),
                @Index(name = "idx_seat_match_section", columnList = "match_id,section_id"),
                @Index(name = "idx_seat_match_grade",   columnList = "match_id,seat_grade_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_seq")
    @SequenceGenerator(name = "seat_seq", sequenceName = "SEAT_SEQ", allocationSize = 50)
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

    /** RESERVED 상태일 때 만료 시각. 스케줄러가 이 값을 보고 만료 처리. */
    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    /** RESERVED 상태일 때 점유한 사용자 ID. 해제 시 본인 검증용. */
    @Column(name = "reserved_by")
    private Long reservedBy;

    /** JPA 낙관적 락 — 점유 충돌 감지용. 트랜잭션 커밋 시 자동 증가. */
    @Version
    @Column(nullable = false)
    private Long version;

    // ==================================================
    // 사용자 기능
    // ==================================================

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

    /**
     * 수정 가능 여부 — AVAILABLE 만 허용 (RESERVED/SOLD 모두 차단)
     * - 결제 진행 중(RESERVED) 이거나 판매 완료(SOLD) 인 좌석은 사용자와 엮여 있어
     *   관리자가 임의로 등급/구역을 바꾸면 결제 정합성이 깨짐
     * - admin 의 좌석 수정/삭제 검증에서도 호출되지만 "상태 질의" 라 사용자 기능으로 분류
     */
    public boolean isEditable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    /**
     * 삭제 가능 여부 — AVAILABLE 만 허용 (RESERVED/SOLD 모두 차단)
     * - 사용자와 엮여 있는 좌석을 지우면 FK 가 깨지거나 환불 처리가 불가능해짐
     * */
    public boolean isDeletable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    // ==================================================
    // 관리자 기능
    // ==================================================

    /**
     * 좌석 생성
     * - 등록 시 status 는 항상 AVAILABLE
     * - admin/seat Service에서 호출
     */
    public static Seat create(Match match, Section section, SeatGrade grade,
                              String rowLabel, int seatNo, String seatCode) {
        Seat s = new Seat();
        s.match = match;
        s.section = section;
        s.seatGrade = grade;
        s.rowLabel = rowLabel;
        s.seatNo = seatNo;
        s.seatCode = seatCode;
        s.status = SeatStatus.AVAILABLE;
        return s;
    }

    /**
     * 좌석 수정 — 구역/등급만 변경
     * - rowLabel/seatNo/seatCode 는 식별값이라 변경하지 않음
     * - 호출 전 isEditable() 로 확인 먼저
     * - admin/seat Service에서 호출
     */
    public void changeSectionAndGrade(Section section, SeatGrade seatGrade) {
        if (section != null) this.section = section;
        if (seatGrade != null) this.seatGrade = seatGrade;
    }
}
