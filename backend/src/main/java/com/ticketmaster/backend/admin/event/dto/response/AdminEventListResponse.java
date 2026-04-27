package com.ticketmaster.backend.admin.event.dto.response;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 이벤트 목록 조회용 간략한 정보 응답
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminEventListResponse {
    private Long eventId;
    private String title;
    private SportType sportType;
    private String place;
    private LocalDate start_date;
    private LocalDate end_date;
    private EventStatus status;

    // Entity -> DTO 변환 메소드
    public static AdminEventListResponse from(Event event) {
        return new AdminEventListResponse(
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
