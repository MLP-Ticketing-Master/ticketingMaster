package com.ticketmaster.backend.domain.booking.repository;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * 관리자용 목록 조회
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.match m
        JOIN FETCH m.event
        JOIN FETCH m.homeTeam
        JOIN FETCH m.awayTeam
        JOIN FETCH b.bookingSeats bs
        JOIN FETCH bs.seat s
        JOIN FETCH s.seatGrade
        WHERE (:status IS NULL OR b.status = :status)
        ORDER BY b.id DESC
    """)
    List<Booking> findAllForAdmin(@Param("status") BookingStatus status);

    /**
     * 관리자용 상세 조회
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.match m
        JOIN FETCH m.event
        JOIN FETCH m.homeTeam
        JOIN FETCH m.awayTeam
        JOIN FETCH b.bookingSeats bs
        JOIN FETCH bs.seat s
        JOIN FETCH s.seatGrade
        WHERE b.id = :id
    """)
    Optional<Booking> findDetailById(@Param("id") Long id);
}

