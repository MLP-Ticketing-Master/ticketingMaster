package com.ticketmaster.backend.domain.match.entity;

// "이 경기 지금 하고 있나?" → 경기 진행 관점
public enum MatchStatus {
    SCHEDULED,  // 경기 예정
    LIVE,       // 경기 진행 중
    FINISHED,   // 경기 종료
    CANCELED    // 경기 취소 (우천, 선수 이슈 등)
}
