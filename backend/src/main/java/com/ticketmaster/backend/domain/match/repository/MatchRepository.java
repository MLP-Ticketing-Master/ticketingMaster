package com.ticketmaster.backend.domain.match.repository;

import com.ticketmaster.backend.domain.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
