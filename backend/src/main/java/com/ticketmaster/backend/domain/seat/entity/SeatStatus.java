package com.ticketmaster.backend.domain.seat.entity;

public enum SeatStatus {
    AVAILABLE,  // 예매 가능
    RESERVED,   // 임시 선점 (결제 진행 중)
    SOLD        // 판매 완료
}
