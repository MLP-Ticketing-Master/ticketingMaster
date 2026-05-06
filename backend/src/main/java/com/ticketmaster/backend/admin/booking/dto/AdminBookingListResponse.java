package com.ticketmaster.backend.admin.booking.dto;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminBookingListResponse {
    private Long bookingId;
    private String bookingNumber;
    private Long userId;
    private String userEmail;
    private String eventTitle;
    private LocalDateTime matchStartAt;
    private int totalPrice;
    private BookingStatus status;
    private LocalDateTime createdAt;

    /**
     * Booking 엔티티를 목록 응답 DTO로 변환
     *Repository에서 fetch join으로 user, match, event를 미리 로딩했다고 가정한다
     * (그렇지 않으면 N+1 발생)
     */
    public static AdminBookingListResponse from(Booking entity) {
        return new AdminBookingListResponse(
                entity.getId(),
                entity.getBookingNumber(),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getMatch().getEvent().getTitle(),
                entity.getMatch().getStartAt(),
                entity.getTotalPrice(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
