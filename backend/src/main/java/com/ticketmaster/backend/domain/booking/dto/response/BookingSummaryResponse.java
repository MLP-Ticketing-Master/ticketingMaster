package com.ticketmaster.backend.domain.booking.dto.response;

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
public class BookingSummaryResponse {

    private Long bookingId;
    private String bookingNumber;

    private String eventTitle;
    private LocalDateTime matchStartAt;
    private String roundLabel;

    private List<String> seatCodes;
    private int seatCount;

    private int totalPrice;
    private BookingStatus status;

    private LocalDateTime createdAt;

    public static BookingSummaryResponse from(Booking booking) {
        List<String> seatCodes = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getSeatCode())
                .toList();

        return new BookingSummaryResponse(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getMatch().getEvent().getTitle(),
                booking.getMatch().getStartAt(),
                booking.getMatch().getRoundLabel(),
                seatCodes,
                seatCodes.size(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}