package com.ticketmaster.backend.domain.booking.controller;

import com.ticketmaster.backend.domain.booking.dto.request.BookingCancelRequest;
import com.ticketmaster.backend.domain.booking.dto.request.BookingCreateRequest;
import com.ticketmaster.backend.domain.booking.dto.response.BookingCancelResponse;
import com.ticketmaster.backend.domain.booking.dto.response.BookingResponse;
import com.ticketmaster.backend.domain.booking.dto.response.BookingSummaryResponse;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.service.BookingService;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * 예매 생성 (POST /bookings)
     * JWT 인증 필수 (ROLE_USER)
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid BookingCreateRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        BookingResponse response = bookingService.createBooking(userId, request);
        return ResponseEntity
                .created(URI.create("/bookings/" + response.getBookingId()))
                .body(response);
    }

    /**
     * 단건 조회 (GET /bookings/{bookingId})
     * - 본인 예매만 조회 가능 (ADMIN 제외)
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookingId
    ) {
        Long userId = userDetails.getUser().getId();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        BookingResponse response = bookingService.getBooking(userId, isAdmin, bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 예매 목록 (GET /bookings/me)
     * status 쿼리 파라미터로 필터 (생략 시 전체)
     */
    @GetMapping("/me")
    public ResponseEntity<Page<BookingSummaryResponse>> getMyBookings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = userDetails.getUser().getId();
        Page<BookingSummaryResponse> response = bookingService.getMyBookings(userId, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 예매 취소 (POST /bookings/{bookingId}/cancel)
     * JWT 인증 필수 (ROLE_USER)
     */
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingCancelResponse> cancelBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long bookingId,
            @RequestBody @Valid BookingCancelRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        BookingCancelResponse response = bookingService.cancelBooking(userId, bookingId, request);
        return ResponseEntity.ok(response);
    }
}