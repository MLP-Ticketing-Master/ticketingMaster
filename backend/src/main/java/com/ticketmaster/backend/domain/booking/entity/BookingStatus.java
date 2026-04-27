package com.ticketmaster.backend.domain.booking.entity;

public enum BookingStatus {
    PENDING,    // 결제 대기 (예매 시도, 결제 진행 중)
    CONFIRMED,  // 예매 확정 (결제 완료)
    CANCELED    // 예매 취소 (사용자 취소 / 결제 실패 / 타임아웃)
}
