package com.ticketmaster.backend.domain.seat.controller;

import com.ticketmaster.backend.domain.seat.dto.request.SeatReleaseRequest;
import com.ticketmaster.backend.domain.seat.dto.request.SeatReserveRequest;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReleaseResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReserveResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SeatSectionListResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SectionSeatListResponse;
import com.ticketmaster.backend.domain.seat.service.SeatReservationService;
import com.ticketmaster.backend.domain.seat.service.SeatService;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 좌석 조회
 * <p>
 * GET /matches/{matchId}/sections                    — 1단계: 구역 목록 + 등급별 잔여
 * GET /matches/{matchId}/sections/{sectionId}/seats  — 2단계: 구역 내 좌석
 * <p>
 * 점유 / 해제
 * POST   /matches/{matchId}/seats/reserve
 * DELETE /matches/{matchId}/seats/reserve
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/matches/{matchId}")
public class SeatController {

    private final SeatService seatService;
    private final SeatReservationService seatReservationService;

    // ─── 조회 ────────────────────────────────────────

    /**
     * 좌석 선택 1단계 — 구역 목록 + 등급별 잔여
     */
    @GetMapping("/sections")
    public SeatSectionListResponse findSections(@PathVariable Long matchId) {
        return seatService.findSectionsByMatch(matchId);
    }

    /**
     * 좌석 선택 2단계 — 구역 내 좌석
     */
    @GetMapping("/sections/{sectionId}/seats")
    public SectionSeatListResponse findSeats(@PathVariable Long matchId,
                                             @PathVariable Long sectionId) {
        return seatService.findSeatsBySection(matchId, sectionId);
    }

    // ─── 점유 / 해제 ────────────────────────────────────────

    /**
     * 좌석 일괄 점유
     */
    @PostMapping("/seats/reserve")
    @PreAuthorize("hasRole('USER')")
    public SeatReserveResponse reserve(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SeatReserveRequest request) {
        Long userId = principal.getUser().getId();
        return seatReservationService.reserve(matchId, userId, request.getSeatIds());
    }

    /**
     * 본인 점유 좌석 해제
     */
    @DeleteMapping("/seats/reserve")
    @PreAuthorize("hasRole('USER')")
    public SeatReleaseResponse release(@PathVariable Long matchId,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       @Valid @RequestBody SeatReleaseRequest request) {
        Long userId = principal.getUser().getId();
        return seatReservationService.release(matchId, userId, request.getSeatIds());
    }
}
