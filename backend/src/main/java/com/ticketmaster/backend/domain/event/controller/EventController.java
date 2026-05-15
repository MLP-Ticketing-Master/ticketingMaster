package com.ticketmaster.backend.domain.event.controller;

import com.ticketmaster.backend.domain.event.dto.response.EventDetailResponse;
import com.ticketmaster.backend.domain.event.dto.response.EventListResponse;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    /**
     * 대회 목록 조회 (카드형)
     */
    @GetMapping("")
    public ResponseEntity<Page<EventListResponse>> getEventList(
            @RequestParam(required = false) SportType sportType, // 종목별 필터링용 (필수 아님)
            @RequestParam(required = false) EventStatus status,  // 상태별 필터링용 (필수 아님)
            @PageableDefault(sort = "createdAt") Pageable pageable
    ){
        Page<EventListResponse> page = eventService.getEventList(sportType, status, pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * 대회 상세 조회
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }
}
