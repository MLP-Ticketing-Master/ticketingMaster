package com.ticketmaster.backend.admin.event.dto.response;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.SportType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 이벤트 상세 관리 페이지용 모든 정보 응답
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminEventDetailResponse {
    private String title;
    private SportType sportType;
    private String place;
    private String thumbnailUrl;
    private String detailImageUrl;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String matchDurationText;
    private String ageRating;
    private LocalDateTime bookingOpenAt;
    private LocalDateTime bookingCloseAt;
    private String bookingNotice;
    private Integer maxTicketsPerUser; // ★ 중요: int가 아니라 Integer(래퍼 클래스)를 사용해야 null 값을 받을 수 있습니다!
    private LocalDateTime cancelAvailableUntil;
    private Integer cancelFee;

    public static AdminEventDetailResponse from(Event event) {
        return new AdminEventDetailResponse(
                event.getTitle(),
                event.getSportType(),
                event.getPlace(),
                event.getThumbnailUrl(),
                event.getDetailImageUrl(),
                event.getDescription(),
                event.getStartDate(),
                event.getEndDate(),
                event.getMatchDurationText(),
                event.getAgeRating(),
                event.getBookingOpenAt(),
                event.getBookingCloseAt(),
                event.getBookingNotice(),
                event.getMaxTicketsPerUser(),
                event.getCancelAvailableUntil(),
                event.getCancelFee()
        );
    }
}
