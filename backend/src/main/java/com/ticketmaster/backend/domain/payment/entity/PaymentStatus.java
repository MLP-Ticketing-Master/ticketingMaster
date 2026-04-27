package com.ticketmaster.backend.domain.payment.entity;

public enum PaymentStatus {
    READY,      // 결제 준비 (결제창 생성, 사용자 결제 진행 중)
    SUCCESS,    // 결제 성공 (PG사 승인 완료)
    FAILED,     // 결제 실패 (카드 한도 초과, 잔액 부족 등)
    CANCELED    // 결제 취소 (사용자 취소 / 환불)
}
