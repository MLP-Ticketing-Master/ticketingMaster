package com.ticketmaster.backend.domain.event.entity;

// "이 대회 티켓을 살 수 있나?" → 예매 관점
public enum EventStatus {
    UPCOMING,   // 예매 예정
    OPEN,       // 예매 진행 중
    CLOSED,     // 예매 종료
    FINISHED    // 대회 종료
}
