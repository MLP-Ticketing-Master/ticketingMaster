package com.ticketmaster.backend.admin.event.dto.response;

import com.ticketmaster.backend.domain.event.entity.Event;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminEventResponse {
    private Long eventId;

    // Entity -> DTO 변환 메서드
    public static AdminEventResponse from(Event event) {
        return new AdminEventResponse(
                event.getId()
        );
    }
}
