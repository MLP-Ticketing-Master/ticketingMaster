package com.ticketmaster.backend.domain.booking.scheduler;


import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PENDING Booking 자동 취소 스케줄러 단위 테스트")
class PendingBookingExpirationSchedulerTest {

    @Mock
    BookingRepository bookingRepository;
    @InjectMocks
    PendingBookingExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "seatTtlSeconds", 420L);
    }

    private Booking pendingBooking() {
        Booking b = BeanUtils.instantiateClass(Booking.class);
        ReflectionTestUtils.setField(b, "status", BookingStatus.PENDING);
        return b;
    }

    @Test
    @DisplayName("7분 경과 PENDING → EXPIRED 전환")
    void 만료() {
        // given
        Booking b = pendingBooking();
        given(bookingRepository.findByStatusAndCreatedAtBefore(any(), any(LocalDateTime.class)))
                .willReturn(List.of(b));

        // when
        scheduler.expirePendingBookings();

        // then
        assertThat(b.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(b.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("대상 없음 → 아무 일도 안 함")
    void 대상_없음() {
        // given
        given(bookingRepository.findByStatusAndCreatedAtBefore(any(), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when & then
        scheduler.expirePendingBookings();
    }

    @Test
    @DisplayName("다수 Booking 동시 만료 → 모두 일괄 EXPIRED")
    void 다수_일괄_만료() {
        // given
        List<Booking> bookings = List.of(pendingBooking(), pendingBooking(), pendingBooking());
        given(bookingRepository.findByStatusAndCreatedAtBefore(any(), any(LocalDateTime.class)))
                .willReturn(bookings);

        // when
        scheduler.expirePendingBookings();

        // then
        for (Booking b : bookings) {
            assertThat(b.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        }
    }
}