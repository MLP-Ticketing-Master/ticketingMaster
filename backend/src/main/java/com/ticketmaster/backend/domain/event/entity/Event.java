package com.ticketmaster.backend.domain.event.entity;

import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
}
