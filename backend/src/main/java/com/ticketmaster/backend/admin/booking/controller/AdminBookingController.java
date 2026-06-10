package com.ticketmaster.backend.admin.booking.controller;

import com.ticketmaster.backend.admin.booking.dto.response.AdminBookingDetailResponse;
import com.ticketmaster.backend.admin.booking.dto.response.AdminBookingListResponse;
import com.ticketmaster.backend.admin.booking.service.AdminBookingService;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 예매", description = "관리자 예매 전체 조회 / 상세 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService service;

    /** 예매 전체 조회 (status 필터) — 200 OK */
    @Operation(summary = "예매 전체 조회", description = "전체 예매 목록 — status 필터 (선택) + 페이지네이션")
    @GetMapping("/bookings")
    public ResponseEntity<Page<AdminBookingListResponse>> getAllListBooking(
            @Parameter(description = "예매 상태 필터 (선택)") @RequestParam(required = false) BookingStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.getAllListBooking(status,pageable));
    }

    /** 예매 상세 조회 */
    @Operation(summary = "예매 상세 조회", description = "예매 ID로 관리자용 상세 정보 반환")
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<AdminBookingDetailResponse> getDetailBooking(
            @Parameter(description = "예매 ID") @PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getDetailBooking(bookingId));
    }
}
