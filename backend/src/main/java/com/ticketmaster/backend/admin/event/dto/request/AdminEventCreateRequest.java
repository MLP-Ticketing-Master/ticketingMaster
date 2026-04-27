package com.ticketmaster.backend.admin.event.dto.request;

import com.ticketmaster.backend.domain.event.entity.SportType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ==========================================
// 대회 등록 (POST /admin/events)
// ==========================================
@Getter
@Builder
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
    private int cancelFee;
}
