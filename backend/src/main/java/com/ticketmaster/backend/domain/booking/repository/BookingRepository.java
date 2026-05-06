package com.ticketmaster.backend.domain.booking.repository;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * 관리자용 목록 조회 — status 필터
     *
     * {@code @EntityGraph}로 user, match, match.event, match.homeTeam, match.awayTeam을
     * 한 번의 쿼리로 fetch join 한다
     * {@code status}가 null이면 전체 조회
     */
    @EntityGraph(attributePaths = {
            "user",
            "match",
            "match.event",
            "match.homeTeam",
            "match.awayTeam"
    })
    @Query("""
        SELECT b FROM Booking b
        WHERE (:status IS NULL OR b.status = :status)
        ORDER BY b.id DESC
        """)
    List<Booking> findAllForAdmin(@Param("status") BookingStatus status);

    /**
     * 관리자용 상세 조회
     * 목록 조회용 EntityGraph + bookingSeats, seat, seatGrade까지 로딩
     * 상세 응답은 좌석 목록과 등급 코드까지 필요하므로 추가 fetch 가 필요
     */
    @EntityGraph(attributePaths = {
            "user",
            "match",
            "match.event",
            "match.homeTeam",
            "match.awayTeam",
            "bookingSeats",
            "bookingSeats.seat",
            "bookingSeats.seat.seatGrade"
    })
    Optional<Booking> findWithDetailsById(Long id);
}

