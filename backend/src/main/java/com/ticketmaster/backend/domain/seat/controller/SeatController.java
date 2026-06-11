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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "좌석 API", description = "구역 / 좌석 조회 + 좌석 점유 / 해제 (동시성 제어)")
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
    @Operation(summary = "구역 목록 조회", description = "좌석 선택 1단계 — 회차의 구역 목록 + 등급별 잔여 좌석 수 반환")
    @GetMapping("/sections")
    public SeatSectionListResponse findSections(
            @Parameter(description = "회차 ID") @PathVariable Long matchId) {
        return seatService.findSectionsByMatch(matchId);
    }

    /**
     * 좌석 선택 2단계 — 구역 내 좌석
     */
    @Operation(summary = "구역 내 좌석 조회", description = "좌석 선택 2단계 — 특정 구역의 좌석 배치 + 점유 상태 반환")
    @GetMapping("/sections/{sectionId}/seats")
    public SectionSeatListResponse findSeats(@Parameter(description = "회차 ID") @PathVariable Long matchId,
                                             @Parameter(description = "구역 ID") @PathVariable Long sectionId) {
        return seatService.findSeatsBySection(matchId, sectionId);
    }

    // ─── 점유 / 해제 ────────────────────────────────────────

    /**
     * 좌석 일괄 점유
     */
    @Operation(summary = "좌석 점유", description = "선택한 좌석들을 일괄 점유 (낙관적 락 기반 동시성 제어)")
    @PostMapping("/seats/reserve")
    @PreAuthorize("hasRole('USER')")
    public SeatReserveResponse reserve(
            @Parameter(description = "회차 ID") @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SeatReserveRequest request) {
        Long userId = principal.getUser().getId();
        return seatReservationService.reserve(matchId, userId, request.getSeatIds());
    }

    /**
     * 본인 점유 좌석 해제
     */
    @Operation(summary = "좌석 해제", description = "본인이 점유한 좌석을 해제하여 다시 선택 가능 상태로 전환")
    @DeleteMapping("/seats/reserve")
    @PreAuthorize("hasRole('USER')")
    public SeatReleaseResponse release(@Parameter(description = "회차 ID") @PathVariable Long matchId,
                                       @AuthenticationPrincipal CustomUserDetails principal,
                                       @Valid @RequestBody SeatReleaseRequest request) {
        Long userId = principal.getUser().getId();
        return seatReservationService.release(matchId, userId, request.getSeatIds());
    }
}
