package com.ticketmaster.backend.domain.event.dto.response;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class EventListResponse {
    private Long eventId;
    private String title;
    private SportType sportType;
    private String place;
    private String thumbnailUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private EventStatus status;

    public static EventListResponse from(Event event) {
        return new EventListResponse(
                event.getId(),
                event.getTitle(),
                event.getSportType(),
                event.getPlace(),
                event.getThumbnailUrl(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStatus()
        );
    }
}
