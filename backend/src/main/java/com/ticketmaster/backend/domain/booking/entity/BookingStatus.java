package com.ticketmaster.backend.domain.booking.entity;

public enum BookingStatus {
    PENDING,    // 결제 대기 (예매 시도, 결제 진행 중)
    CONFIRMED,  // 예매 확정 (결제 완료)
    CANCELED,    // 예매 취소 (사용자 / 관리자 취소)
    EXPIRED     // 결제 시간 초과 자동 만료 (시스템)
}
