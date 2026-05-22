package com.ticketmaster.backend.domain.event.entity;

// "이 대회 티켓을 살 수 있나?" → 예매 관점
public enum EventStatus {
    UPCOMING,   // 이벤트 기간 진행 전
    OPEN,       // 이벤트 진행 중
    FINISHED    // 이벤트 기간 종료 후
}
