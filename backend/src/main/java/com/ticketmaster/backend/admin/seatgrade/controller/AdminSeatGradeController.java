package com.ticketmaster.backend.admin.seatgrade.controller;

import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeCreateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeUpdateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.response.AdminSeatGradeResponse;
import com.ticketmaster.backend.admin.seatgrade.service.AdminSeatGradeService;
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
@PreAuthorize("hasRole('ADMIN')")   // SecurityConfig 의 ROLE 명과 일치하는지 확인
public class AdminSeatGradeController {

    private final AdminSeatGradeService service;

    /** 대회별 좌석 등급 목록 조회 — 200 OK */
    @GetMapping("/events/{eventId}/seat-grades")
    public ResponseEntity<List<AdminSeatGradeResponse>> findAll(@PathVariable Long eventId) {
        return ResponseEntity.ok(service.findAllByEvent(eventId));
    }

    /** 좌석 등급 등록 — 201 Created */
    @PostMapping("/events/{eventId}/seat-grades")
    public ResponseEntity<AdminSeatGradeResponse> create(
            @PathVariable Long eventId,
            @RequestBody @Valid AdminSeatGradeCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(eventId, req));
    }

    /** 좌석 등급 부분 수정 — 200 OK */
    @PatchMapping("/seat-grades/{seatGradeId}")
    public ResponseEntity<AdminSeatGradeResponse> update(
            @PathVariable Long seatGradeId,
            @RequestBody @Valid AdminSeatGradeUpdateRequest req) {
        return ResponseEntity.ok(service.update(seatGradeId, req));
    }

    /** 좌석 등급 삭제 — 204 No Content */
    @DeleteMapping("/seat-grades/{seatGradeId}")
    public ResponseEntity<Void> delete(@PathVariable Long seatGradeId) {
        service.delete(seatGradeId);
        return ResponseEntity.noContent().build();
    }
}
