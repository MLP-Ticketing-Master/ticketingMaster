package com.ticketmaster.backend.domain.match.dto;

import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;

import java.time.LocalDateTime;

/**
 * 예매 가능 검증에 필요한 최소 필드만 담는 캐시 전용 값 객체
 * 엔티티를 캐시에 넣으면 lazy 연관 접근이 깨지므로, 검증에 쓰는 값만 보관
 */
public record MatchBookingGate(
        EventStatus eventStatus,
        MatchStatus matchStatus,
        LocalDateTime bookingOpenAt,
        LocalDateTime bookingCloseAt
) {
    // Match.isBookableAt 과 동일한 3축 게이팅을 캐시 값만으로 수행
    public boolean isBookableAt(LocalDateTime now) {
        return eventStatus == EventStatus.OPEN
                && matchStatus == MatchStatus.SCHEDULED
                && !now.isBefore(bookingOpenAt)
                && !now.isAfter(bookingCloseAt);
    }
}
