package com.ticketmaster.backend.admin.booking.controller;

import com.ticketmaster.backend.admin.booking.dto.AdminBookingDetailResponse;
import com.ticketmaster.backend.admin.booking.dto.AdminBookingListResponse;
import com.ticketmaster.backend.admin.booking.service.AdminBookingService;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService service;

    /** 예매 전체 조회 (status 필터) — 200 OK */
    @GetMapping("/bookings")
    public ResponseEntity<List<AdminBookingListResponse>> getAllListBooking(
            @RequestParam(required = false) BookingStatus status) {
        return ResponseEntity.ok(service.getAllListBooking(status));
    }

    /** 예매 상세 조회 — 200 OK */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<AdminBookingDetailResponse> getDetailBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getDetailBooking(bookingId));
    }
}
