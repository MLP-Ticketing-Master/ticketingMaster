package com.ticketmaster.backend.domain.event.repository;

import com.ticketmaster.backend.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByTitle(String s);
}
