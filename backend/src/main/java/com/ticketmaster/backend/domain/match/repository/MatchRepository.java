package com.ticketmaster.backend.domain.match.repository;

import com.ticketmaster.backend.domain.match.entity.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
    Page<Match> findByEventId(Long eventId, Pageable pageable);
}
