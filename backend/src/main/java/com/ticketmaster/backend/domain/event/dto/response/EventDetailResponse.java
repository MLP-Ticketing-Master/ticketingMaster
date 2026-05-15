package com.ticketmaster.backend.domain.event.dto.response;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.match.dto.MatchResponse;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.seat.dto.response.SeatGradeResponse;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class EventDetailResponse {
    // 이벤트 기본 정보
    private String title;
    private SportType sportType;
    private String place;
    private String detailImageUrl;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String matchDurationText;
    private String ageRating;
    private String bookingNotice;
    private int maxTicketsPerUser;
    private LocalDateTime cancelAvailableUntil;
    private int cancelFee;
    private EventStatus status;

    // 좌석 등급 정보
    List<SeatGradeResponse> seatGrades;

    // 하위 매치 정보
    List<MatchResponse> matches;

    // Entity -> DTO
    public static EventDetailResponse from(Event event) {
        return EventDetailResponse.builder()
                .title(event.getTitle())
                .sportType(event.getSportType())
                .place(event.getPlace())
                .detailImageUrl(event.getDetailImageUrl())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .matchDurationText(event.getMatchDurationText())
                .ageRating(event.getAgeRating())
                .bookingNotice(event.getBookingNotice())
                .maxTicketsPerUser(event.getMaxTicketsPerUser())
                .cancelAvailableUntil(event.getCancelAvailableUntil())
                .cancelFee(event.getCancelFee())
                .status(event.getStatus())
                .seatGrades(
                        event.getSeatGrades().stream()
                        .map(sg -> SeatGradeResponse.from(sg))
                        .toList()
                )
                .matches(
                        event.getMatches().stream()
                        .map(m -> MatchResponse.from(m))
                        .toList()
                )
                .build();
    }
}