package com.ticketmaster.backend.admin.booking.dto.response;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingSeat;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.team.entity.Team;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminBookingDetailResponse {
    private Long bookingId;
    private String bookingNumber;

    // 고객 정보
    private Long userId;
    private String userNickname;
    private String userEmail;

    // 대회 정보
    private String eventTitle;
    private MatchInfo matchInfo;

    // 좌석 정보
    private List<SeatInfo> seats;

    // 결제/상태
    private int totalPrice;
    private BookingStatus status;

    private LocalDateTime createdAt;

    public static AdminBookingDetailResponse from(Booking entity) {
        Match match = entity.getMatch();

        List<SeatInfo> seatInfos = entity.getBookingSeats().stream()
                .map(SeatInfo::from)
                .toList();

        return new AdminBookingDetailResponse(
                entity.getId(),
                entity.getBookingNumber(),
                entity.getUser().getId(),
                entity.getUser().getNickname(),
                entity.getUser().getEmail(),
                match.getEvent().getTitle(),
                MatchInfo.from(match),
                seatInfos,
                entity.getTotalPrice(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    /**
     * 회차 정보 중첩 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MatchInfo {
        private Long matchId;
        private LocalDateTime startAt;
        private String roundLabel;       // ex) 결승 1경기
        private String homeTeamName;
        private String awayTeamName;

        public static MatchInfo from(Match match) {
            return new MatchInfo(
                    match.getId(),
                    match.getStartAt(),
                    match.getRoundLabel(),
                    teamNameOrNull(match.getHomeTeam()),
                    teamNameOrNull(match.getAwayTeam())
            );
        }

        /** 팀이 null이면(대진 미정) null 반환 */
        private static String teamNameOrNull(Team team) {
            return team == null ? null : team.getName();
        }
    }

    /**
     * 좌석 정보 중첩 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SeatInfo {
        private Long seatId;
        private String seatCode;
        private String gradeCode;
        private int seatPrice;

        public static SeatInfo from(BookingSeat bookingSeat) {
            return new SeatInfo(
                    bookingSeat.getSeat().getId(),
                    bookingSeat.getSeat().getSeatCode(),
                    bookingSeat.getSeat().getSeatGrade().getGradeCode(),
                    bookingSeat.getSeatPrice()  // 스냅샷 가격
            );
        }
    }
}
