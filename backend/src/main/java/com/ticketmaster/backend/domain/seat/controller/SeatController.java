package com.ticketmaster.backend.domain.seat.controller;

import com.ticketmaster.backend.domain.seat.dto.response.SeatSectionListResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SectionSeatListResponse;
import com.ticketmaster.backend.domain.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 좌석 조회
 *
 * GET /matches/{matchId}/sections                    — 1단계: 구역 목록 + 등급별 잔여
 * GET /matches/{matchId}/sections/{sectionId}/seats  — 2단계: 구역 내 좌석
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/matches/{matchId}")
public class SeatController {

    private final SeatService seatService;

    /** 좌석 선택 1단계 — 구역 목록 + 등급별 잔여 */
    @GetMapping("/sections")
    public SeatSectionListResponse findSections(@PathVariable Long matchId) {
        return seatService.findSectionsByMatch(matchId);
    }

    /** 좌석 선택 2단계 — 구역 내 좌석 */
    @GetMapping("/sections/{sectionId}/seats")
    public SectionSeatListResponse findSeats(@PathVariable Long matchId,
                                             @PathVariable Long sectionId) {
        return seatService.findSeatsBySection(matchId, sectionId);
    }
}
