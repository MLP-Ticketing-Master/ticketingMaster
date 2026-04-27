package com.ticketmaster.backend.domain.event.entity;

import com.ticketmaster.backend.admin.event.dto.request.AdminEventUpdateRequest;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 30)
    private SportType sportType;

    @Column(nullable = false, length = 200)
    private String place;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "detail_image_url", length = 500)
    private String detailImageUrl;

    @Lob
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "match_duration_text", length = 100)
    private String matchDurationText;

    @Column(name = "age_rating", length = 30)
    private String ageRating;

    @Column(name = "booking_open_at", nullable = false)
    private LocalDateTime bookingOpenAt;

    @Column(name = "booking_close_at", nullable = false)
    private LocalDateTime bookingCloseAt;

    @Column(name = "booking_notice", length = 300)
    private String bookingNotice;

    @Column(name = "max_tickets_per_user", nullable = false)
    private int maxTicketsPerUser;

    @Column(name = "cancel_available_until")
    private LocalDateTime cancelAvailableUntil;

    @Column(name = "cancel_fee")
    private int cancelFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    /**
     * 생성자 (빌더)
     */
    @Builder
    public Event(String title, SportType sportType, String place, String thumbnailUrl,
                 String detailImageUrl, String description, LocalDate startDate,
                 LocalDate endDate, String matchDurationText, String ageRating,
                 LocalDateTime bookingOpenAt, LocalDateTime bookingCloseAt,
                 String bookingNotice, int maxTicketsPerUser,
                 LocalDateTime cancelAvailableUntil, int cancelFee, EventStatus status) {
        this.title = title;
        this.sportType = sportType;
        this.place = place;
        this.thumbnailUrl = thumbnailUrl;
        this.detailImageUrl = detailImageUrl;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.matchDurationText = matchDurationText;
        this.ageRating = ageRating;
        this.bookingOpenAt = bookingOpenAt;
        this.bookingCloseAt = bookingCloseAt;
        this.bookingNotice = bookingNotice;
        this.maxTicketsPerUser = maxTicketsPerUser;
        this.cancelAvailableUntil = cancelAvailableUntil;
        this.cancelFee = cancelFee;
        this.status = status;
    }

    // TODO: SeatGrade, Section, Match 와의 양방향 컬렉션이 필요하면
    //       @OneToMany(mappedBy = "event") 로 추가

    // 이벤트 상태 변경 (UPCOMING → OPEN → CLOSED → FINISHED)
    public void changeStatus(EventStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 현재 시각이 예매 가능한 시간인지 체크
     * - status == OPEN
     * - bookingOpenAt <= now <= bookingCloseAt
     *
     * Service에서 false일 때 BusinessException 던지는 용도
     */
    public boolean isBookableAt(LocalDateTime now) {
        return status == EventStatus.OPEN
                && !now.isBefore(bookingOpenAt)
                && !now.isAfter(bookingCloseAt);
    }

    /**
     * 이벤트 정보 수정 (PATCH 방식 대응: null이 아닌 필드만 업데이트)
     */
    public void update(AdminEventUpdateRequest request) {
        if (request.getTitle() != null) this.title = request.getTitle();
        if (request.getSportType() != null) this.sportType = request.getSportType();
        if (request.getPlace() != null) this.place = request.getPlace();
        if (request.getThumbnailUrl() != null) this.thumbnailUrl = request.getThumbnailUrl();
        if (request.getDetailImageUrl() != null) this.detailImageUrl = request.getDetailImageUrl();
        if (request.getDescription() != null) this.description = request.getDescription();
        if (request.getStartDate() != null) this.startDate = request.getStartDate();
        if (request.getEndDate() != null) this.endDate = request.getEndDate();
        if (request.getMatchDurationText() != null) this.matchDurationText = request.getMatchDurationText();
        if (request.getAgeRating() != null) this.ageRating = request.getAgeRating();
        if (request.getBookingOpenAt() != null) this.bookingOpenAt = request.getBookingOpenAt();
        if (request.getBookingCloseAt() != null) this.bookingCloseAt = request.getBookingCloseAt();
        if (request.getBookingNotice() != null) this.bookingNotice = request.getBookingNotice();

        // 주의: DTO에서 숫자형 데이터는 반드시
        // int가 아닌 Integer(래퍼 클래스)로 선언되어 있어야 아래처럼 null 체크가 가능
        if (request.getMaxTicketsPerUser() != null) this.maxTicketsPerUser = request.getMaxTicketsPerUser();
        if (request.getCancelAvailableUntil() != null) this.cancelAvailableUntil = request.getCancelAvailableUntil();
        if (request.getCancelFee() != null) this.cancelFee = request.getCancelFee();
    }
}
