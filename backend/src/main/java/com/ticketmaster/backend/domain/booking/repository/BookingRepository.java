package com.ticketmaster.backend.domain.booking.repository;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // -------------------------------------------------------
    // 관리자용
    // -------------------------------------------------------

    boolean existsByMatch_Event_IdAndStatusNot(Long eventId, BookingStatus status);

    boolean existsByMatch_IdAndStatusNot(Long matchId, BookingStatus status);

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
     * 내 예매 목록 — 사용자 노출용 (CONFIRMED / CANCELED 만)
     * PENDING / EXPIRED 는 내부 상태로 사용자에게 미노출 (운영자용은 findAllForAdmin 참조)
     * status 가 null 이면 CONFIRMED + CANCELED 둘 다 반환, 지정 시 해당 상태로 추가 필터
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
              AND b.status IN ('CONFIRMED', 'CANCELED')
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.id DESC
            """)
    Page<Booking> findMyBookings(
            @Param("userId") Long userId,
            @Param("status") BookingStatus status,
            Pageable pageable);


    /**
     * 결제 승인 시 Booking + BookingSeat + Seat 일괄 로딩
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            LEFT JOIN FETCH b.bookingSeats bs
            LEFT JOIN FETCH bs.seat
            WHERE b.id = :id
            """)
    Optional<Booking> findForPayment(@Param("id") Long id);

    /**
     * 예매 취소 시 Booking + BookingSeat + Seat + Match + Payment 일괄 로딩
     * PaymentService.refund() 호출 전 Payment 필요
     */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.match m
            LEFT JOIN FETCH b.bookingSeats bs
            LEFT JOIN FETCH bs.seat
            WHERE b.id = :id
            """)
    Optional<Booking> findForCancel(@Param("id") Long id);

    /** PENDING 상태 + 좌석 TTL 지난 Booking 목록 (자동 만료 스케줄러용) */
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime threshold);

    /**
     * 예매 생성 멱등성 검증용 — 같은 user + match 의 PENDING booking 을 좌석까지 함께 로딩
     * BookingService.createBooking() 에서 요청 seatIds 와 정확히 일치하는 기존 PENDING 이 있으면 재사용
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
            WHERE b.user.id = :userId
              AND b.match.id = :matchId
              AND b.status = 'PENDING'
            """)
    List<Booking> findPendingForIdempotency(@Param("userId") Long userId,
                                           @Param("matchId") Long matchId);
}