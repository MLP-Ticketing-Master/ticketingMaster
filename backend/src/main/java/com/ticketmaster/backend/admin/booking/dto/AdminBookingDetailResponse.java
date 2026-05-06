package com.ticketmaster.backend.admin.booking.dto;

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
    private Long userId;
    private String userEmail;
    private String eventTitle;
    private MatchInfo matchInfo;
    private List<SeatInfo> seats;
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
     *
     * 홈/원정 팀은 nullable (대진 미정 가능)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MatchInfo {
        private Long matchId;
        private LocalDateTime startAt;
        private String homeTeamName;
        private String awayTeamName;

        public static MatchInfo from(Match match) {
            return new MatchInfo(
                    match.getId(),
                    match.getStartAt(),
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
     * 좌석 정보 중첩 DTO.
     *
     * <p>가격은 BookingSeat의 스냅샷 가격을 사용한다.
     * (SeatGrade의 현재 가격과 다를 수 있음 — 결제 정합성 보호)</p>
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
