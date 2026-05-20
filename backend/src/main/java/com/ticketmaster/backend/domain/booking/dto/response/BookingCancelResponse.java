package com.ticketmaster.backend.domain.booking.dto.response;

import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 예매 취소 응답 DTO
 * POST /bookings/{bookingId}/cancel
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BookingCancelResponse {

    private Long bookingId;
    private BookingStatus bookingStatus;

    /** 원결제 금액 */
    private int originalAmount;

    /** 취소 수수료 (24h~3일: 10%, 3일 이상: 0%) */
    private int cancelFee;

    /** 실제 환불 금액 = originalAmount - cancelFee */
    private int refundAmount;

    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;

    public static BookingCancelResponse of(
            Long bookingId,
            int originalAmount,
            int cancelFee,
            int refundAmount,
            LocalDateTime canceledAt,
            LocalDateTime refundedAt
    ) {
        return new BookingCancelResponse(
                bookingId,
                BookingStatus.CANCELED,
                originalAmount,
                cancelFee,
                refundAmount,
                canceledAt,
                refundedAt
        );
    }
}