package com.ticketmaster.backend.admin.event.controller;

import com.ticketmaster.backend.admin.event.dto.request.AdminEventCreateRequest;
import com.ticketmaster.backend.admin.event.dto.request.AdminEventUpdateRequest;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventDetailResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventListResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventResponse;
import com.ticketmaster.backend.admin.event.service.AdminEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEventController {
    private final AdminEventService eventService;

    /**
     * 이벤트 등록
     */
    @PostMapping
    public ResponseEntity<AdminEventResponse> createEvent(@Valid @RequestBody AdminEventCreateRequest request) {
        AdminEventResponse response = eventService.createEvent(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 이벤트 목록 조회 (간단한 정보 리스트)
     */
    @GetMapping
    public ResponseEntity<Page<AdminEventListResponse>> getEventList(Pageable pageable) {
        return ResponseEntity.ok(eventService.getEventList(pageable));
    }

    /**
     * 이벤트 상세 조회 (상세 정보 리스트)
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<AdminEventDetailResponse> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }

    /**
     * 이벤트 수정 요청
     */
    @PatchMapping("/{eventId}")
    public ResponseEntity<AdminEventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody AdminEventUpdateRequest request) {
        AdminEventResponse response = eventService.updateEvent(eventId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 이벤트 삭제
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);

        return ResponseEntity.noContent().build();
    }
}
