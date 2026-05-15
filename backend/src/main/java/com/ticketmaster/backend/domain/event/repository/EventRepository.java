package com.ticketmaster.backend.domain.event.repository;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByTitle(String s);

    @Query(
        countQuery = """
            SELECT count(e)
            FROM Event e
            WHERE e.deletedAt IS NULL
                AND (:sportType IS NULL OR e.sportType = :sportType) 
                AND (:status IS NULL OR e.status = :status)
        """)
    Page<Event> findAllBySportTypeAndStatusExceptDeleted(@Param("sportType") SportType sportType,
                                            @Param("status") EventStatus status,
                                            Pageable pageable);


    @Query("""
    SELECT e FROM Event e
    LEFT JOIN FETCH e.matches m
    LEFT JOIN FETCH m.homeTeam
    LEFT JOIN FETCH m.awayTeam
    WHERE :id = e.id
    """)
    Optional<Event> getEventDetailById(Long id);
}
