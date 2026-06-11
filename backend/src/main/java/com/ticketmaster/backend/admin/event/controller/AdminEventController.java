package com.ticketmaster.backend.admin.event.controller;

import com.ticketmaster.backend.admin.event.dto.request.AdminEventCreateRequest;
import com.ticketmaster.backend.admin.event.dto.request.AdminEventUpdateRequest;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventDetailResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventListResponse;
import com.ticketmaster.backend.admin.event.dto.response.AdminEventResponse;
import com.ticketmaster.backend.admin.event.service.AdminEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 이벤트", description = "관리자 이벤트 등록 / 조회 / 수정 / 삭제")
@RestController
@RequestMapping("/admin/events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEventController {
    private final AdminEventService eventService;

    /**
     * 이벤트 등록
     */
    @Operation(summary = "이벤트 등록", description = "신규 이벤트(대회) 생성")
    @PostMapping
    public ResponseEntity<AdminEventResponse> createEvent(@Valid @RequestBody AdminEventCreateRequest request) {
        AdminEventResponse response = eventService.createEvent(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 이벤트 목록 조회 (간단한 정보 리스트)
     */
    @Operation(summary = "이벤트 목록 조회", description = "관리자용 이벤트 목록 (페이지네이션)")
    @GetMapping
    public ResponseEntity<Page<AdminEventListResponse>> getEventList(Pageable pageable) {
        return ResponseEntity.ok(eventService.getEventList(pageable));
    }

    /**
     * 이벤트 상세 조회 (상세 정보 리스트)
     */
    @Operation(summary = "이벤트 상세 조회", description = "이벤트 ID로 관리자용 상세 정보 반환")
    @GetMapping("/{eventId}")
    public ResponseEntity<AdminEventDetailResponse> getEventDetail(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventDetail(eventId));
    }

    /**
     * 이벤트 수정 요청
     */
    @Operation(summary = "이벤트 수정", description = "이벤트 정보 부분 수정")
    @PatchMapping("/{eventId}")
    public ResponseEntity<AdminEventResponse> updateEvent(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId,
            @Valid @RequestBody AdminEventUpdateRequest request) {
        AdminEventResponse response = eventService.updateEvent(eventId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 이벤트 삭제
     */
    @Operation(summary = "이벤트 삭제", description = "이벤트 삭제 (소프트 삭제)")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@Parameter(description = "이벤트 ID") @PathVariable Long eventId) {
        eventService.deleteEvent(eventId);

        return ResponseEntity.noContent().build();
    }
}
