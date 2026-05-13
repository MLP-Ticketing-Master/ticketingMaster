package com.ticketmaster.backend.domain.seat.scheduler;


import com.ticketmaster.backend.domain.seat.service.SeatReservationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatExpirySchedulerTest {

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private SeatExpiryScheduler scheduler;

    @Test
    @DisplayName("정상 호출 — service 를 1회 호출")
    void 정상_호출() {
        // given
        given(seatReservationService.expireOverdueReservations()).willReturn(3);

        // when
        scheduler.expireOverdueReservations();

        // then
        verify(seatReservationService, times(1)).expireOverdueReservations();
    }

    @Test
    @DisplayName("service 예외 발생 시 — 스케줄러는 예외를 삼킴 (다음 주기 보호)")
    void 예외_삼킴() {
        // given
        given(seatReservationService.expireOverdueReservations())
                .willThrow(new RuntimeException("DB 일시 오류"));

        // when & then — 예외가 위로 전파되면 스케줄러 스레드가 죽어 다음 주기에 영향
        assertThatNoException()
                .isThrownBy(scheduler::expireOverdueReservations);
    }
}