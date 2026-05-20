package com.ticketmaster.backend.domain.queue.service;


import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.dto.response.QueueStatusResponse;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
        ReflectionTestUtils.setField(queueService, "sessionSeconds", 600);
    }

    @Test
    @DisplayName("TC-01: 정상 진입 → 토큰 + 순번 1 + DB 이력 저장 호출")
    void 정상_진입() {
        // given
        Match match = mock(Match.class);
        given(match.isBookableAt(any(LocalDateTime.class))).willReturn(true);
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
        Match match = mock(Match.class);
        given(match.isBookableAt(any(LocalDateTime.class))).willReturn(true);
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
        // given — isBookableAt 이 false 반환 (아직 오픈 안 됨 / 마감 / event/match 상태 부적합)
        Match match = mock(Match.class);
        given(match.isBookableAt(any(LocalDateTime.class))).willReturn(false);
        given(matchRepository.findById(1L)).willReturn(Optional.of(match));

        // when & then
        assertThatThrownBy(() -> queueService.enter(1L, 1000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_OPEN);
    }

    @Nested
    @DisplayName("대기열 상태 조회 — getStatus")
    class GetStatus {

        @Test
        @DisplayName("TC-01: WAITING 상태 → status=WAITING, queueNumber/remainingAhead 정상")
        void waiting_상태() {
            // given
            Long matchId = 1L;
            String token = "token-waiting";

            // 회차 있음
            given(matchRepository.existsById(matchId)).willReturn(true);

            // 토큰 메타 (Hash) — matchId 일치, 진입 시각 임의값
            long enteredAtMs = 1700000000000L;
            Map<String, String> meta = new HashMap<>();
            meta.put("matchId", String.valueOf(matchId));
            meta.put("enteredAt", String.valueOf(enteredAtMs));
            given(queueRedis.getTokenMeta(token)).willReturn(meta);

            // ALLOWED 아님, WAITING 명단에 들어있음 (100번째)
            given(queueRedis.isAllowed(matchId, token)).willReturn(false);
            given(queueRedis.isWaiting(matchId, token)).willReturn(true);
            given(queueRedis.getRank(matchId, token)).willReturn(100L);

            // when
            QueueStatusResponse response = queueService.getStatus(matchId, token);

            // then — WAITING 응답에 순번 정보가 모두 들어있어야 함
            assertThat(response.getStatus()).isEqualTo("WAITING");
            assertThat(response.getQueueNumber()).isEqualTo(100L);
            assertThat(response.getRemainingAhead()).isEqualTo(99L);
            assertThat(response.getEstimatedWaitSeconds()).isNotNull();
            // ALLOWED 관련 필드는 null 이어야 함
            assertThat(response.getAllowedAt()).isNull();
            assertThat(response.getEntryDeadline()).isNull();
        }

        @Test
        @DisplayName("TC-02: ALLOWED 상태 → status=ALLOWED, allowedAt/entryDeadline 정상")
        void allowed_상태() {
            // given
            Long matchId = 1L;
            String token = "token-allowed";
            given(matchRepository.existsById(matchId)).willReturn(true);

            // 토큰 메타에 enteredAt + allowedAt 둘 다 있는 상태 (스케줄러가 채워줌)
            long enteredAtMs = 1700000000000L;
            long allowedAtMs = 1700000600000L;  // enteredAt 보다 600 초(10분) 뒤
            Map<String, String> meta = new HashMap<>();
            meta.put("matchId", String.valueOf(matchId));
            meta.put("enteredAt", String.valueOf(enteredAtMs));
            meta.put("allowedAt", String.valueOf(allowedAtMs));
            given(queueRedis.getTokenMeta(token)).willReturn(meta);

            // ALLOWED 키 존재
            given(queueRedis.isAllowed(matchId, token)).willReturn(true);

            // when
            QueueStatusResponse response = queueService.getStatus(matchId, token);

            // then — ALLOWED 응답에 시간 정보가 채워져야 함
            assertThat(response.getStatus()).isEqualTo("ALLOWED");
            assertThat(response.getAllowedAt()).isNotNull();
            // entryDeadline = allowedAt + sessionSeconds(600)
            assertThat(response.getEntryDeadline()).isAfter(response.getAllowedAt());
            // 순번 관련은 null
            assertThat(response.getQueueNumber()).isNull();
            assertThat(response.getRemainingAhead()).isNull();
        }

        @Test
        @DisplayName("TC-03: 토큰 메타 없음 (Redis Hash 만료) → QUEUE_TOKEN_EXPIRED")
        void 토큰메타_없음() {
            // given
            given(matchRepository.existsById(1L)).willReturn(true);
            // Hash 키 자체가 사라진 상태 (TTL 만료) → 빈 Map 반환
            given(queueRedis.getTokenMeta("expired")).willReturn(new HashMap<>());

            // when & then
            assertThatThrownBy(() -> queueService.getStatus(1L, "expired"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("TC-04: matchId 불일치 → QUEUE_TOKEN_MATCH_MISMATCH")
        void matchId_불일치() {
            // given
            given(matchRepository.existsById(1L)).willReturn(true);
            // 토큰의 matchId는 99 인데 요청은 1
            Map<String, String> meta = new HashMap<>();
            meta.put("matchId", "99");
            meta.put("enteredAt", "1700000000000");
            given(queueRedis.getTokenMeta("tk")).willReturn(meta);

            // when & then
            assertThatThrownBy(() -> queueService.getStatus(1L, "tk"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_TOKEN_MATCH_MISMATCH);
        }

        @Test
        @DisplayName("TC-05: matchId 없는 회차 → MATCH_NOT_FOUND")
        void 매치_없음() {
            // given — 회차 자체가 DB 에 없음
            given(matchRepository.existsById(999L)).willReturn(false);

            // when & then — 첫 단계에서 바로 예외 (토큰까지 안 감)
            assertThatThrownBy(() -> queueService.getStatus(999L, "tk"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);

        }

        @Test
        @DisplayName("TC-06: 토큰 null/blank → QUEUE_TOKEN_NOT_FOUND")
        void 토큰_누락() {
            // given
            given(matchRepository.existsById(1L)).willReturn(true);

            // when & then — 토큰 null
            assertThatThrownBy(() -> queueService.getStatus(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_TOKEN_NOT_FOUND);

            // when & then — 토큰 빈 문자열 (공백만)
            assertThatThrownBy(() -> queueService.getStatus(1L, "   "))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUEUE_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("TC-07: ALLOWED/WAITING 둘 다 아님 → status=EXPIRED")
        void 권한_만료() {
            // given
            given(matchRepository.existsById(1L)).willReturn(true);
            // Hash 메타는 살아있는데 (30분 TTL 안 끝남) ALLOWED 키도 SortedSet 도 없음
            // → ALLOWED 키 10분 TTL 이 먼저 만료된 상태
            Map<String, String> meta = new HashMap<>();
            meta.put("matchId", "1");
            meta.put("enteredAt", "1700000000000");
            given(queueRedis.getTokenMeta("tk")).willReturn(meta);
            given(queueRedis.isAllowed(1L, "tk")).willReturn(false);
            given(queueRedis.isWaiting(1L, "tk")).willReturn(false);

            // when
            QueueStatusResponse response = queueService.getStatus(1L, "tk");

            // then — EXPIRED 응답은 enteredAt 만 채워지고 나머지는 null
            assertThat(response.getStatus()).isEqualTo("EXPIRED");
            assertThat(response.getEnteredAt()).isNotNull();
            assertThat(response.getQueueNumber()).isNull();
            assertThat(response.getAllowedAt()).isNull();
        }
    }
}