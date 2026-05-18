package com.ticketmaster.backend.domain.booking.entity;

import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예매–좌석 N:M 매핑 + 가격 스냅샷
 *
 * (match_id, seat_id) UNIQUE 제약으로 동일 좌석의 이중 예매를 DB 레벨에서 차단
 * → DataIntegrityViolationException → GlobalExceptionHandler → SEAT_ALREADY_RESERVED (409)
 */
@Getter
@Entity
@Table(
        name = "booking_seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_booking_seat_match_seat",
                        columnNames = {"match_id", "seat_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingSeat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seat_seq")
    @SequenceGenerator(name = "booking_seat_seq", sequenceName = "BOOKING_SEAT_SEQ", allocationSize = 50)
    private Long id;

    /** 어느 예매에 속한 좌석인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** 어떤 좌석인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    /**
     * UNIQUE 제약의 match_id 컬럼 — Seat.match.id 를 비정규화해서 저장
     * Seat → Match FK 를 타지 않고도 DB가 유니크 검사 가능
     */
    @Column(name = "match_id", nullable = false)
    private Long matchId;

    /** 예매 시점 가격 스냅샷 (등급 가격 변동에도 유지) */
    @Column(name = "seat_price", nullable = false)
    private int seatPrice;

    public static BookingSeat of(Seat seat, int seatPrice) {
        BookingSeat bs = new BookingSeat();
        bs.seat = seat;
        bs.matchId = seat.getMatch().getId();
        bs.seatPrice = seatPrice;
        return bs;
    }

    /** Booking.addBookingSeat() 안에서 호출 (양방향 동기화) */
    void assignBooking(Booking booking) {
        this.booking = booking;
    }
}