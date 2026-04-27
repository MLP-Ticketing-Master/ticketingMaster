package com.ticketmaster.backend.admin.event.dto.request;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ==========================================
// 대회 수정 (PATCH /admin/events/{eventId})
// ==========================================
@Getter
@Builder
public class AdminEventUpdateRequest {
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

    // DTO -> Entity 변환 메소드
    public Event toEntity() {
        return Event.builder()
                .title(this.title)
                .sportType(this.sportType)
                .place(this.place)
                .thumbnailUrl(this.thumbnailUrl)
                .detailImageUrl(this.detailImageUrl)
                .description(this.description)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .matchDurationText(this.matchDurationText)
                .ageRating(this.ageRating)
                .bookingOpenAt(this.bookingOpenAt)
                .bookingCloseAt(this.bookingCloseAt)
                .bookingNotice(this.bookingNotice)
                .maxTicketsPerUser(this.maxTicketsPerUser)
                .cancelAvailableUntil(this.cancelAvailableUntil)
                .cancelFee(this.cancelFee)
                .status(EventStatus.UPCOMING) // 디폴트 status 설정
                .build();
    }
}
