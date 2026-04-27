package com.ticketmaster.backend.domain.queue.entity;

public enum QueueStatus {
    WAITING,    // 대기열 진입, 순서 대기 중
    ALLOWED,    // 입장 허용 (예매 페이지 접근 가능)
    ENTERED,    // 예매 페이지 진입 완료
    EXPIRED     // 입장 시간 만료 / 대기 포기
}
