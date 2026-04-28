package com.ticketmaster.backend.admin.event.dto.request;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ==========================================
// 대회 등록 (POST /admin/events)
// ==========================================
@Getter
@NoArgsConstructor
public class AdminEventCreateRequest {
    @NotBlank(message = "타이틀은 필수입니다.")
    private String title;

    @NotNull(message = "종목 타입은 필수입니다.")
    private SportType sportType;

    @NotBlank(message = "장소는 필수입니다.")
    private String place;

    private String thumbnailUrl;
    private String detailImageUrl;
    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    private String matchDurationText;
    private String ageRating;

    @NotNull(message = "예매 시작 시간은 필수입니다.")
    private LocalDateTime bookingOpenAt;

    @NotNull(message = "예매 종료 시간은 필수입니다.")
    private LocalDateTime bookingCloseAt;

    private String bookingNotice;

    @Min(value = 1, message = "최소 1장 이상이어야 합니다.")
    private int maxTicketsPerUser; // 등록 시에는 필수 입력이므로 기본형 int 사용 가능

    private LocalDateTime cancelAvailableUntil;

    @Min(0)
    private int cancelFee;

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
