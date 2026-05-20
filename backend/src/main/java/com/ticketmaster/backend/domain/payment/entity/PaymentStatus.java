package com.ticketmaster.backend.domain.payment.entity;

public enum PaymentStatus {
    SUCCESS,    // 결제 성공 (PG사 승인 완료)
    FAILED,     // 결제 실패 (가상계좌 입금 실패, 토스 승인 거절, 검증 실패 등)
    CANCELED    // 결제 취소 (사용자 취소 / 환불)
}
