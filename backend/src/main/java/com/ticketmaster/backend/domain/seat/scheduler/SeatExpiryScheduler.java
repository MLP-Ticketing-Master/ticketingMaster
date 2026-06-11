package com.ticketmaster.backend.domain.seat.scheduler;


import com.ticketmaster.backend.domain.seat.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 좌석 점유를 30초마다 자동으로 풀어주는 스케줄러
 *
 * 실제 만료 처리는 SeatReservationService 가 하고, 여기서는 30초마다 한 번씩 호출만 함
 * 한 번 실패해도 30초 뒤에 다시 실행되도록 try/catch 로 예외 잡기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatExpiryScheduler {

    private final SeatReservationService seatReservationService;

    /**
     * 스케줄러 실행 간격은 application.yaml 의 seat.expiry-scan-interval-ms 값으로 설정
     */
    @Scheduled(fixedDelayString = "${seat.expiry-scan-interval-ms}")
    public void expireOverdueReservations() {
        try {
            int expired = seatReservationService.expireOverdueReservations();
            if (expired > 0) {
                log.info("[SeatExpiry] 만료 처리 완료 — count={}", expired);
            }
        } catch (Exception e) {
            // 여기서 예외를 잡아주지 않으면 스케줄러 자체가 멈춤. 잡아주면 30초 뒤에 다시 돌아감
            log.error("[SeatExpiry] 만료 처리 중 예외", e);
        }
    }
}
