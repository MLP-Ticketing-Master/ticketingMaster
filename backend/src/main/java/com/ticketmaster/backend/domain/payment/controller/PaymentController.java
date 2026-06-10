package com.ticketmaster.backend.domain.payment.controller;

import com.ticketmaster.backend.domain.payment.dto.request.PaymentConfirmRequest;
import com.ticketmaster.backend.domain.payment.dto.response.PaymentDetailResponse;
import com.ticketmaster.backend.domain.payment.dto.response.PaymentResponse;
import com.ticketmaster.backend.domain.payment.service.PaymentService;
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
 * 결제 API
 * - POST /payments/confirm — 토스 위젯에서 받은 paymentKey 로 결제 확정
 * - GET  /payments/{paymentId} — 결제 상세 조회
 */
@Tag(name = "결제 API", description = "토스페이먼츠 결제 승인 / 결제 상세 조회")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 승인 — 토스 위젯에서 받은 paymentKey / orderId / amount 로 결제 확정
     */
    @Operation(summary = "결제 승인", description = "토스 위젯에서 받은 paymentKey / orderId / amount 로 결제 확정")
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('USER')")
    public PaymentResponse confirm(@Valid @RequestBody PaymentConfirmRequest request,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getId();
        return paymentService.confirm(request, userId);
    }

    /**
     * 결제 상세 조회 — 본인 결제만 조회 (마이페이지)
     */
    @Operation(summary = "결제 상세 조회", description = "결제 ID로 상세 조회 (본인 결제만, 마이페이지)")
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('USER')")
    public PaymentDetailResponse getDetail(@Parameter(description = "결제 ID") @PathVariable Long paymentId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getId();
        return paymentService.getPaymentDetail(paymentId, userId);
    }
}
