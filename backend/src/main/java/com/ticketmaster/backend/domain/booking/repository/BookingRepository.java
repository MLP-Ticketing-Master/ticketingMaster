package com.ticketmaster.backend.domain.booking.repository;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    // "Match의 Event의 id가 같고, Status가 주어진 값과 '다른(Not)' 예매가 존재하는가?"
    boolean existsByMatch_Event_IdAndStatusNot(Long eventId, BookingStatus status);
}
