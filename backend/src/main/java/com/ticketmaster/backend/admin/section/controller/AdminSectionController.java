package com.ticketmaster.backend.admin.section.controller;

import com.ticketmaster.backend.admin.section.dto.request.AdminSectionCreateRequest;
import com.ticketmaster.backend.admin.section.dto.request.AdminSectionUpdateRequest;
import com.ticketmaster.backend.admin.section.dto.response.AdminSectionResponse;
import com.ticketmaster.backend.admin.section.service.AdminSectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSectionController {

    private final AdminSectionService service;

    /** 대회별 구역 목록 조회 — 200 OK */
    @GetMapping("/events/{eventId}/sections")
    public ResponseEntity<List<AdminSectionResponse>> findAll(@PathVariable Long eventId) {
        return ResponseEntity.ok(service.findAllByEvent(eventId));
    }

    /** 구역 등록  - 201 Created */
    @PostMapping("/events/{eventId}/sections")
    public ResponseEntity<AdminSectionResponse> create(
            @PathVariable Long eventId,
            @RequestBody @Valid AdminSectionCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(eventId, req));
    }

    /** 구역 부분 수정 — 200 OK */
    @PatchMapping("/sections/{sectionId}")
    public ResponseEntity<AdminSectionResponse> update(
            @PathVariable Long sectionId,
            @RequestBody @Valid AdminSectionUpdateRequest req) {
        return ResponseEntity.ok(service.update(sectionId, req));
    }

    /** 구역 삭제 — 204 No Content */
    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<Void> delete(@PathVariable Long sectionId) {
        service.delete(sectionId);
        return ResponseEntity.noContent().build();
    }
}
