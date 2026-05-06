package com.ticketmaster.backend.admin.booking.controller;

import com.ticketmaster.backend.admin.booking.dto.response.AdminBookingListResponse;
import com.ticketmaster.backend.admin.booking.service.AdminBookingService;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService service;

    /** 예매 전체 조회 (status 필터) — 200 OK */
    @GetMapping("/bookings")
    public ResponseEntity<Page<AdminBookingListResponse>> getAllListBooking(
            @RequestParam(required = false) BookingStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.getAllListBooking(status,pageable));
    }

//    /** 예매 상세 조회 — 200 OK */
//    @GetMapping("/bookings/{bookingId}")
//    public ResponseEntity<AdminBookingDetailResponse> getDetailBooking(@PathVariable Long bookingId) {
//        return ResponseEntity.ok(service.getDetailBooking(bookingId));
//    }
}
