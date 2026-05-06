package com.ticketmaster.backend.admin.booking.dto;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminBookingListResponse {
    private Long bookingId;
    private String bookingNumber;

    // 고객 정보
    private Long userId;
    private String userNickname;     // ex) 홍길동
    private String userEmail;        // ex) hong@example.com

    // 대회/회차 정보
    private String eventTitle;       // ex) LOL 챔피언스 코리아 2026 스프링 결승
    private LocalDateTime matchStartAt;
    private String roundLabel;       // ex) 결승 1경기 (nullable)

    // 좌석 정보 (요약)
    private List<String> seatCodes;  // ex) ["VIP-A5-12"] or ["B3-8", "B3-9"]
    private int seatCount;           // 좌석 개수

    // 결제/상태
    private int totalPrice;
    private BookingStatus status;

    private LocalDateTime createdAt;

    /**
     * Booking 엔티티를 목록 응답 DTO로 변환
     *Repository에서 fetch join으로 user, match, event를 미리 로딩했다고 가정
     */
    public static AdminBookingListResponse from(Booking entity) {
        // 좌석 코드 리스트 생성 (예: ["VIP-A5-12", "VIP-A5-13"])
        List<String> seatCodes = entity.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getSeatCode())
                .toList();


        return new AdminBookingListResponse(
                entity.getId(),
                entity.getBookingNumber(),
                entity.getUser().getId(),
                entity.getUser().getNickname(),
                entity.getUser().getEmail(),
                entity.getMatch().getEvent().getTitle(),
                entity.getMatch().getStartAt(),
                entity.getMatch().getRoundLabel(),
                seatCodes,
                seatCodes.size(),
                entity.getTotalPrice(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
