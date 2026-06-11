package com.ticketmaster.backend.admin.seatgrade.controller;

import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeCreateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeUpdateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.response.AdminSeatGradeResponse;
import com.ticketmaster.backend.admin.seatgrade.service.AdminSeatGradeService;
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

@Tag(name = "관리자 - 좌석등급", description = "관리자 좌석 등급 조회 / 등록 / 수정 / 삭제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")   // SecurityConfig 의 ROLE 명과 일치하는지 확인
public class AdminSeatGradeController {

    private final AdminSeatGradeService service;

    /** 대회별 좌석 등급 목록 조회 — 200 OK */
    @Operation(summary = "좌석 등급 목록 조회", description = "대회(이벤트)별 좌석 등급 목록 반환")
    @GetMapping("/events/{eventId}/seat-grades")
    public ResponseEntity<List<AdminSeatGradeResponse>> findAll(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId) {
        return ResponseEntity.ok(service.findAllByEvent(eventId));
    }

    /** 좌석 등급 등록 — 201 Created */
    @Operation(summary = "좌석 등급 등록", description = "대회에 신규 좌석 등급 생성")
    @PostMapping("/events/{eventId}/seat-grades")
    public ResponseEntity<AdminSeatGradeResponse> create(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId,
            @RequestBody @Valid AdminSeatGradeCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(eventId, req));
    }

    /** 좌석 등급 부분 수정 — 200 OK */
    @Operation(summary = "좌석 등급 수정", description = "좌석 등급 정보 부분 수정")
    @PatchMapping("/seat-grades/{seatGradeId}")
    public ResponseEntity<AdminSeatGradeResponse> update(
            @Parameter(description = "좌석 등급 ID") @PathVariable Long seatGradeId,
            @RequestBody @Valid AdminSeatGradeUpdateRequest req) {
        return ResponseEntity.ok(service.update(seatGradeId, req));
    }

    /** 좌석 등급 삭제 — 204 No Content */
    @Operation(summary = "좌석 등급 삭제", description = "좌석 등급 삭제")
    @DeleteMapping("/seat-grades/{seatGradeId}")
    public ResponseEntity<Void> delete(@Parameter(description = "좌석 등급 ID") @PathVariable Long seatGradeId) {
        service.delete(seatGradeId);
        return ResponseEntity.noContent().build();
    }
}
