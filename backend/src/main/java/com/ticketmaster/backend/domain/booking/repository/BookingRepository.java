package com.ticketmaster.backend.domain.booking.repository;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // -------------------------------------------------------
    // 관리자용
    // -------------------------------------------------------

    boolean existsByMatch_Event_IdAndStatusNot(Long eventId, BookingStatus status);

    @Query("""
    SELECT b FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.match m
        LEFT JOIN FETCH m.event
        LEFT JOIN FETCH m.homeTeam
        LEFT JOIN FETCH m.awayTeam
        WHERE (:status IS NULL OR b.status = :status)
        ORDER BY b.id DESC
    """)
    Page<Booking> findAllForAdmin(@Param("status") BookingStatus status, Pageable pageable);

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

    // -------------------------------------------------------
    // 사용자용
    // -------------------------------------------------------

    /**
     * 단건 상세 조회 — bookingSeats/seat/seatGrade/match/event fetch join
     * BookingService.getBooking() 에서 사용
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.match m
        JOIN FETCH m.event
        LEFT JOIN FETCH m.homeTeam
        LEFT JOIN FETCH m.awayTeam
        JOIN FETCH b.bookingSeats bs
        JOIN FETCH bs.seat s
        JOIN FETCH s.seatGrade
        WHERE b.id = :id
    """)
    Optional<Booking> findDetailByIdForUser(@Param("id") Long id);

    /**
     * 내 예매 목록 — status 필터 + 최신순
     * status가 null이면 전체 조회
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.match m
        JOIN FETCH m.event
        LEFT JOIN FETCH m.homeTeam
        LEFT JOIN FETCH m.awayTeam
        JOIN FETCH b.bookingSeats bs
        JOIN FETCH bs.seat s
        JOIN FETCH s.seatGrade
        WHERE b.user.id = :userId
          AND (:status IS NULL OR b.status = :status)
        ORDER BY b.id DESC
    """)
    Page<Booking> findMyBookings(
            @Param("userId") Long userId,
            @Param("status") BookingStatus status,
            Pageable pageable);
}