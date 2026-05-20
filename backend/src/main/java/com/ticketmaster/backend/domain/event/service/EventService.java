package com.ticketmaster.backend.domain.event.service;

import com.ticketmaster.backend.domain.event.dto.response.EventDetailResponse;
import com.ticketmaster.backend.domain.event.dto.response.EventListResponse;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;

    /**
     * 이벤트 목록 조회 (카드형)
     */
    public Page<EventListResponse> getEventList(SportType sportType, EventStatus status, Pageable pageable) {
        Page<Event> eventList = eventRepository.findAllBySportTypeAndStatusExceptDeleted(sportType, status, pageable);

        // Entity -> DTO
        Page<EventListResponse> eventDtoList = eventList.map(event -> EventListResponse.from(event));

        return eventDtoList;
    }

    /**
     * 이벤트 상세 조회
     */
    public EventDetailResponse getEventDetail(Long eventId) {
        Event event = eventRepository.getEventDetailById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        if (event.getStatus() == EventStatus.UPCOMING && !isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return EventDetailResponse.from(event);
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
