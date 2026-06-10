package com.ticketmaster.backend.admin.section.controller;

import com.ticketmaster.backend.admin.section.dto.request.AdminSectionCreateRequest;
import com.ticketmaster.backend.admin.section.dto.request.AdminSectionUpdateRequest;
import com.ticketmaster.backend.admin.section.dto.response.AdminSectionResponse;
import com.ticketmaster.backend.admin.section.service.AdminSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 구역", description = "관리자 구역 조회 / 등록 / 수정 / 삭제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSectionController {

    private final AdminSectionService service;

    /** 대회별 구역 목록 조회 — 200 OK */
    @Operation(summary = "구역 목록 조회", description = "대회(이벤트)별 구역 목록 반환")
    @GetMapping("/events/{eventId}/sections")
    public ResponseEntity<List<AdminSectionResponse>> findAll(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId) {
        return ResponseEntity.ok(service.findAllByEvent(eventId));
    }

    /** 구역 등록  - 201 Created */
    @Operation(summary = "구역 등록", description = "대회에 신규 구역 생성")
    @PostMapping("/events/{eventId}/sections")
    public ResponseEntity<AdminSectionResponse> create(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId,
            @RequestBody @Valid AdminSectionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(eventId, req));
    }

    /** 구역 부분 수정 — 200 OK */
    @Operation(summary = "구역 수정", description = "구역 정보 부분 수정")
    @PatchMapping("/sections/{sectionId}")
    public ResponseEntity<AdminSectionResponse> update(
            @Parameter(description = "구역 ID") @PathVariable Long sectionId,
            @RequestBody @Valid AdminSectionUpdateRequest req) {
        return ResponseEntity.ok(service.update(sectionId, req));
    }

    /** 구역 삭제 — 204 No Content */
    @Operation(summary = "구역 삭제", description = "구역 삭제 (하드 삭제)")
    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<Void> delete(@Parameter(description = "구역 ID") @PathVariable Long sectionId) {
        service.delete(sectionId);
        return ResponseEntity.noContent().build();
    }
}
