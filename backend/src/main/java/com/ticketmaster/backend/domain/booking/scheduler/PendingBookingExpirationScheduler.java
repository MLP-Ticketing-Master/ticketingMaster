package com.ticketmaster.backend.domain.booking.scheduler;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PENDING 상태로 좌석 TTL(7분) 지난 Booking 을 자동 EXPIRED로 전환
 *
 * 좌석 자체는 스케줄러가 이미 AVAILABLE 로 되돌려놓으므로,
 * 이 스케줄러는 "좌석은 풀렸는데 PENDING 으로 남은" 고아 Booking 만 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingBookingExpirationScheduler {

    private final BookingRepository bookingRepository;

    @Value("${seat.reservation-ttl-seconds:420}")
    private long seatTtlSeconds;

    /**
     * 1분 주기로 실행 — 좌석 TTL(7분) 지난 PENDING Booking 일괄 EXPIRED 전환
     * initialDelay 로 부팅 직후 SeedData reset 트랜잭션과의 락 경합(데드락) 회피
     */
    @Scheduled(
            fixedDelayString = "${booking.expiry-scan-interval-ms}",
            initialDelayString = "${booking.expiry-scan-initial-delay-ms}"
    )
    @Transactional
    public void expirePendingBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(seatTtlSeconds);
        List<Booking> targets = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING, threshold);

        if (targets.isEmpty()) {
            return;
        }

        for (Booking b : targets) {
            b.expire();
        }

        log.info("[PendingBookingExpiration] 자동 만료 완료 count={}", targets.size());
    }
}
