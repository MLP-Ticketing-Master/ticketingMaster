package com.ticketmaster.backend.domain.booking.repository;

import com.ticketmaster.backend.domain.booking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
}
