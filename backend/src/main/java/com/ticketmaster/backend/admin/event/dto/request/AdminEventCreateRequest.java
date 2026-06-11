package com.ticketmaster.backend.admin.event.dto.request;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// ==========================================
// 대회 등록 (POST /admin/events)
// ==========================================
@Getter
@Builder // 테스트 코드에서 객체를 쉽게 생성하기 위해 추가!
@NoArgsConstructor
@AllArgsConstructor // @Builder를 클래스 레벨에 쓰려면 이 어노테이션이 세트로 필요합니다.
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

    private String bookingNotice;

    @NotNull(message = "1인당 최대 예매 수량은 필수입니다.")
    @Min(value = 1, message = "최소 1장 이상이어야 합니다.")
    @Max(value = 2, message = "최대 2장까지 가능합니다.")
    private int maxTicketsPerUser; // 등록 시에는 필수 입력이므로 기본형 int 사용 가능

    @NotNull(message = "취소 수수료는 필수입니다.")
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
                .bookingNotice(this.bookingNotice)
                .maxTicketsPerUser(this.maxTicketsPerUser)
                .cancelFee(this.cancelFee)
                .status(EventStatus.UPCOMING) // 디폴트 status 설정
                .build();
    }
}
