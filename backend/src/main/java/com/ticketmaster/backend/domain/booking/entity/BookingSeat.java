package com.ticketmaster.backend.domain.booking.entity;

import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예매–좌석 N:M 매핑 + 가격 스냅샷
 * <p>
 * 좌석 점유 불변량은 Seat.status + @Version 에서 강제 — BookingSeat 는 "이 예매에 어떤 좌석이 들어갔었나" 의 기록
 */
@Getter
@Entity
@Table(name = "booking_seats")
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

    /** 예매 시점 가격 스냅샷 (등급 가격 변동에도 유지) */
    @Column(name = "seat_price", nullable = false)
    private int seatPrice;

    public static BookingSeat of(Seat seat, int seatPrice) {
        BookingSeat bs = new BookingSeat();
        bs.seat = seat;
        bs.seatPrice = seatPrice;
        return bs;
    }

    /** Booking.addBookingSeat() 안에서 호출 (양방향 동기화) */
    void assignBooking(Booking booking) {
        this.booking = booking;
    }
}
