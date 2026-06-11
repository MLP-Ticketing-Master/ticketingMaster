package com.ticketmaster.backend.admin.event.dto.response;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminEventResponse {
    private Long eventId;
    private String title;
    private SportType sportType;
    private String place;
    private LocalDate startDate;
    private LocalDate endDate;
    private EventStatus status;

    // Entity -> DTO 변환 메서드
    public static AdminEventResponse from(Event event) {
        return new AdminEventResponse(
                event.getId(),
                event.getTitle(),
                event.getSportType(),
                event.getPlace(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStatus()
        );
    }
}
