package com.ticketmaster.backend.domain.booking.controller;

import com.ticketmaster.backend.domain.booking.dto.request.BookingCancelRequest;
import com.ticketmaster.backend.domain.booking.dto.request.BookingCreateRequest;
import com.ticketmaster.backend.domain.booking.dto.response.BookingCancelResponse;
import com.ticketmaster.backend.domain.booking.dto.response.BookingResponse;
import com.ticketmaster.backend.domain.booking.dto.response.BookingSummaryResponse;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.service.BookingService;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "예매 API", description = "예매 생성 / 조회 / 취소")
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * 예매 생성 (POST /bookings)
     * JWT 인증 필수 (ROLE_USER)
     */
    @Operation(summary = "예매 생성", description = "점유한 좌석으로 예매 생성 — 결제 대기(PENDING) 상태로 시작")
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
    @Operation(summary = "예매 단건 조회", description = "예매 ID로 상세 조회 (본인 예매만, ADMIN 은 전체 허용)")
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "예매 ID") @PathVariable Long bookingId
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
    @Operation(summary = "내 예매 목록 조회", description = "로그인 사용자의 예매 목록 — status 필터 + 페이지네이션")
    @GetMapping("/me")
    public ResponseEntity<Page<BookingSummaryResponse>> getMyBookings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "예매 상태 필터 (선택)") @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = userDetails.getUser().getId();
        Page<BookingSummaryResponse> response = bookingService.getMyBookings(userId, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 본인 매치별 미완료 예매 조회 (GET /bookings/me/pending?matchId=X)
     * 매치 페이지 진입 시 호출 — PENDING 있으면 결제 단계로 자동 복귀시키는 용도
     * 있으면 200 + BookingResponse, 없으면 204 No Content
     */
    @Operation(summary = "미완료 예매 조회", description = "매치별 PENDING 예매 조회 — 있으면 200 + 예매 정보, 없으면 204")
    @GetMapping("/me/pending")
    public ResponseEntity<BookingResponse> getMyPending(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "회차 ID") @RequestParam Long matchId
    ) {
        Long userId = userDetails.getUser().getId();
        return bookingService.getMyPending(userId, matchId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * 예매 취소 (POST /bookings/{bookingId}/cancel)
     * JWT 인증 필수 (ROLE_USER)
     */
    @Operation(summary = "예매 취소", description = "본인 예매 취소 + 좌석 반환 (취소 사유 포함)")
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingCancelResponse> cancelBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "예매 ID") @PathVariable Long bookingId,
            @RequestBody @Valid BookingCancelRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        BookingCancelResponse response = bookingService.cancelBooking(userId, bookingId, request);
        return ResponseEntity.ok(response);
    }
}