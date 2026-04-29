package com.ticketmaster.backend.admin.seat.controller;

import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatBulkCreateRequest;
import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatCreateRequest;
import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatUpdateRequest;
import com.ticketmaster.backend.admin.seat.dto.response.AdminSeatResponse;
import com.ticketmaster.backend.admin.seat.service.AdminSeatService;
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
public class AdminSeatController {

    private final AdminSeatService service;

    /**
     * 단건 등록 — 201
     */
    @PostMapping("/matches/{matchId}/seats")
    public ResponseEntity<AdminSeatResponse> create(
            @PathVariable Long matchId,
            @RequestBody @Valid AdminSeatCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(matchId, req));
    }

    /**
     * 일괄 등록 — 201 좌석 그리드 초기 세팅용 (최대 5000석), 응답은 등록된 좌석 리스트
     */
    @PostMapping("/matches/{matchId}/seats/bulk")
    public ResponseEntity<List<AdminSeatResponse>> bulkCreate(
            @PathVariable Long matchId,
            @RequestBody @Valid AdminSeatBulkCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bulkCreate(matchId, req));
    }

    /**
     * 회차 좌석 전체 조회 - 200
     */
    @GetMapping("/matches/{matchId}/seats")
    public List<AdminSeatResponse> findAll(@PathVariable Long matchId) {
        return service.findAllByMatch(matchId);
    }

    /**
     * 좌석 수정 — 200 응답은 SeatResponse (전체 상태)
     */
    @PatchMapping("/seats/{seatId}")
    public AdminSeatResponse update(
            @PathVariable Long seatId,
            @RequestBody @Valid AdminSeatUpdateRequest req) {
        return service.update(seatId, req);
    }

    /**
     * 좌석 삭제 — 204
     */
    @DeleteMapping("/seats/{seatId}")
    public ResponseEntity<Void> delete(@PathVariable Long seatId) {
        service.delete(seatId);
        return ResponseEntity.noContent().build();
    }
}
