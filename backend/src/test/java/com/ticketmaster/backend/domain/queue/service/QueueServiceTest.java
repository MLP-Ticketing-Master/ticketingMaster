package com.ticketmaster.backend.domain.queue.service;


import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * TK-82 대기열 진입 서비스 단위 테스트
 *
 * Mockito 로 Repository / Redis 를 가짜 객체로 대체해서 분기만 빠르게 검증
 * 실제 동시성 / Redis 통합 검증은 QueueEntryIT 에서
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 진입 서비스 단위 테스트")
class QueueServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private QueueRedisRepository queueRedis;

    @InjectMocks
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        // @Value 는 단위 테스트에서 주입되지 않으므로 ReflectionTestUtils 로 직접 세팅
        ReflectionTestUtils.setField(queueService, "admissionBatchSize", 200);
        ReflectionTestUtils.setField(queueService, "admissionIntervalSeconds", 30);
        ReflectionTestUtils.setField(queueService, "tokenTtlSeconds", 1800);
    }

    @Test
    @DisplayName("TC-01: 정상 진입 → 토큰 + 순번 1 + DB 이력 저장 호출")
    void 정상_진입() {
        // given
        Event event = mock(Event.class);
        given(event.getBookingOpenAt()).willReturn(LocalDateTime.now().minusDays(1));
        Match match = mock(Match.class);
        given(match.getEvent()).willReturn(event);
        given(matchRepository.findById(1L)).willReturn(Optional.of(match));
        User user = mock(User.class);
        given(userRepository.findById(1000L)).willReturn(Optional.of(user));

        // Redis 가 1번 순번을 반환하도록 설정
        given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong()))
                .willReturn(1L);

        // when
        QueueEnterResponse response = queueService.enter(1L, 1000L);

        // then
        assertThat(response.getQueueToken()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getQueueNumber()).isEqualTo(1L);
        assertThat(response.getRemainingAhead()).isZero();
        assertThat(response.getEstimatedWaitSeconds()).isZero();

        // DB save 호출 여부 확인
        verify(queueRepository).save(any(Queue.class));
    }

    @Test
    @DisplayName("TC-02: 이미 대기 중인 사용자가 재진입 → QUEUE_ALREADY_ENTERED")
    void 중복_진입() {
        // given - Redis 에서 중복 진입 예외 던지도록 설정
        Event event = mock(Event.class);
        given(event.getBookingOpenAt()).willReturn(LocalDateTime.now().minusDays(1));
        Match match = mock(Match.class);
        given(match.getEvent()).willReturn(event);
        given(matchRepository.findById(1L)).willReturn(Optional.of(match));
        User user = mock(User.class);
        given(userRepository.findById(1000L)).willReturn(Optional.of(user));

        given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong()))
                .willThrow(new BusinessException(ErrorCode.QUEUE_ALREADY_ENTERED));

        // when & then
        assertThatThrownBy(() -> queueService.enter(1L, 1000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_ALREADY_ENTERED);

        // Redis 에서 막혔으니 DB 저장은 일어나지 않아야 함
        verify(queueRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: 존재하지 않는 matchId 진입 → MATCH_NOT_FOUND")
    void 존재하지_않는_회차() {
        // given - matchRepository 가 빈 Optional 을 돌려주도록 설정
        given(matchRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queueService.enter(99L, 1000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    @DisplayName("TC-04: 예매 오픈 시간 이전 진입 → BOOKING_NOT_OPEN")
    void 예매_오픈_전_진입() {
        // given — bookingOpenAt 이 미래 (아직 오픈 안 됨)
        Event event = mock(Event.class);
        given(event.getBookingOpenAt()).willReturn(LocalDateTime.now().plusDays(1));
        Match match = mock(Match.class);
        given(match.getEvent()).willReturn(event);
        given(matchRepository.findById(1L)).willReturn(Optional.of(match));

        // when & then
        assertThatThrownBy(() -> queueService.enter(1L, 1000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_OPEN);
    }
}