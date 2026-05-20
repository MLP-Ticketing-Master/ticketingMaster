package com.ticketmaster.backend.domain.event.controller;

import com.ticketmaster.backend.domain.event.dto.response.EventDetailResponse;
import com.ticketmaster.backend.domain.event.dto.response.EventListResponse;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.service.EventService;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
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
     * 이벤트 목록 조회 (카드형)
     */
    @GetMapping("")
    public ResponseEntity<Page<EventListResponse>> getEventList(
            @RequestParam(required = false) SportType sportType, // 종목별 필터링용 (필수 아님)
            @RequestParam(required = false) EventStatus status,  // 상태별 필터링용 (필수 아님)
            @PageableDefault(sort = "createdAt") Pageable pageable
    ){
        // 정렬 조건 검증
        pageable.getSort().forEach(order -> {
                if(!order.getProperty().equals("createdAt") && !order.getProperty().equals("startDate")) {
                    throw new BusinessException(ErrorCode.INVALID_SORT_VALUE);
                }
            }
        );

        Page<EventListResponse> page = eventService.getEventList(sportType, status, pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * 이벤트 상세 조회
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }
}
