package com.ticketmaster.backend.domain.event.controller;

import com.ticketmaster.backend.domain.event.dto.response.EventDetailResponse;
import com.ticketmaster.backend.domain.event.dto.response.EventListResponse;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.service.EventService;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이벤트 API", description = "이벤트(대회) 목록 / 상세 조회")
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    /**
     * 이벤트 목록 조회 (카드형)
     */
    @Operation(summary = "이벤트 목록 조회", description = "종목 / 상태 필터 + 페이지네이션으로 이벤트 카드 목록 반환")
    @GetMapping("")
    public ResponseEntity<Page<EventListResponse>> getEventList(
            @Parameter(description = "종목 필터 (선택)") @RequestParam(required = false) SportType sportType, // 종목별 필터링용 (필수 아님)
            @Parameter(description = "상태 필터 (선택)") @RequestParam(required = false) EventStatus status,  // 상태별 필터링용 (필수 아님)
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
    @Operation(summary = "이벤트 상세 조회", description = "이벤트 ID로 상세 정보 + 매치 목록 반환")
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEventDetail(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }
}
