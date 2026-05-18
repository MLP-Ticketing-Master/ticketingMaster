package com.ticketmaster.backend.domain.booking.dto.response;

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
public class BookingResponse {

    private Long bookingId;
    private String bookingNumber;

    private MatchInfo matchInfo;
    private List<SeatInfo> seats;

    private int totalPrice;
    private BookingStatus status;

    private LocalDateTime createdAt;

    public static BookingResponse from(Booking booking) {
        List<SeatInfo> seatInfos = booking.getBookingSeats().stream()
                .map(SeatInfo::from)
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getBookingNumber(),
                MatchInfo.from(booking.getMatch()),
                seatInfos,
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MatchInfo {
        private Long matchId;
        private String eventTitle;
        private String roundLabel;
        private String homeTeamName;
        private String awayTeamName;
        private LocalDateTime startAt;

        public static MatchInfo from(Match match) {
            return new MatchInfo(
                    match.getId(),
                    match.getEvent().getTitle(),
                    match.getRoundLabel(),
                    teamNameOrNull(match.getHomeTeam()),
                    teamNameOrNull(match.getAwayTeam()),
                    match.getStartAt()
            );
        }

        private static String teamNameOrNull(Team team) {
            return team == null ? null : team.getName();
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SeatInfo {
        private Long seatId;
        private String seatCode;
        private String gradeCode;
        private int seatPrice;   // 예매 시점 스냅샷 가격

        public static SeatInfo from(BookingSeat bookingSeat) {
            return new SeatInfo(
                    bookingSeat.getSeat().getId(),
                    bookingSeat.getSeat().getSeatCode(),
                    bookingSeat.getSeat().getSeatGrade().getGradeCode(),
                    bookingSeat.getSeatPrice()
            );
        }
    }
}