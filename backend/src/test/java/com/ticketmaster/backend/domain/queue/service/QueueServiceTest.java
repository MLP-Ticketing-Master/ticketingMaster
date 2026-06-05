package com.ticketmaster.backend.domain.queue.service;


import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.match.dto.MatchBookingGate;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.match.service.MatchQueryService;
import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.dto.response.QueueStatusResponse;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 대기열 진입 서비스 단위 테스트
 *
 * Mockito 로 Repository / Redis / 협력 서비스를 가짜 객체로 대체해서 분기만 빠르게 검증
 * 실제 동시성 / Redis 통합 검증은 QueueEntryIT 에서
 *
 * 튜닝 후 enter() 흐름 변경 반영
 *  - 매치 검증: matchRepository.findById → matchQueryService.getBookingGate (캐시 게이트)
 *  - 이력 저장: 동기 queueRepository.save → queueHistoryService.saveWaitingHistoryAsync (비동기 디스패치)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 진입 서비스 단위 테스트")
class QueueServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private QueueRedisRepository queueRedis;

    @Mock
    private MatchQueryService matchQueryService;

    @Mock
    private QueueHistoryService queueHistoryService;

    @InjectMocks
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        // @Value 는 단위 테스트에서 주입되지 않으므로 ReflectionTestUtils 로 직접 세팅
        ReflectionTestUtils.setField(queueService, "admissionBatchSize", 200);
        ReflectionTestUtils.setField(queueService, "admissionIntervalSeconds", 30);
        ReflectionTestUtils.setField(queueService, "tokenTtlSeconds", 1800);
        ReflectionTestUtils.setField(queueService, "sessionSeconds", 600);
        ReflectionTestUtils.setField(queueService, "burstEnabled", false);   // 기본은 OFF, burst 검증 케이스에서 true 로 전환
    }

    /**
     * 예매 가능한 게이트 — eventStatus=OPEN, matchStatus=SCHEDULED, 오픈 윈도우 안
     */
    private MatchBookingGate openGate() {
        return new MatchBookingGate(
                EventStatus.OPEN, MatchStatus.SCHEDULED,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));
    }

    @Test
    @DisplayName("TC-01: 정상 진입 → 토큰 + 순번 1 + 이력 비동기 저장 호출")
    void 정상_진입() {
        // given
        given(matchQueryService.getBookingGate(1L)).willReturn(openGate());

        // 신규 진입 시그널 — Redis 가 서비스가 발급한 토큰을 그대로 + 순번 1 로 반환 (allowed=false 일반 WAITING)
        given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong(),
                eq(200), eq(600), eq(false)))
                .willAnswer(inv -> new QueueRedisRepository.EnterResult(inv.getArgument(2), 1L, false));

        // when
        QueueEnterResponse response = queueService.enter(1L, 1000L);

        // then
        assertThat(response.getQueueToken()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getQueueNumber()).isEqualTo(1L);
        assertThat(response.getRemainingAhead()).isZero();
        assertThat(response.getEstimatedWaitSeconds()).isZero();

        // then — 이력은 비동기 서비스로 디스패치 (직접 save 가 아님)
        verify(queueHistoryService).saveWaitingHistoryAsync(
                eq(1000L), eq(1L), anyString(), eq(1L), any(), any(), eq(false));
    }

    @Test
    @DisplayName("TC-02: 동일 사용자 재진입 → 기존 토큰 그대로 반환 + 이력 저장 스킵")
    void 재진입_idempotent() {
        // given
        given(matchQueryService.getBookingGate(1L)).willReturn(openGate());

        // 재진입 시그널 — Redis 가 서비스의 신규 토큰을 무시하고 기존 토큰 / 순번을 그대로 반환 (allowed=false)
        String existingToken = "existing-token";
        given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong(),
                eq(200), eq(600), eq(false)))
                .willReturn(new QueueRedisRepository.EnterResult(existingToken, 42L, false));

        // when
        QueueEnterResponse response = queueService.enter(1L, 1000L);

        // then — 응답에 기존 토큰과 순번이 그대로 실려야 함
        assertThat(response.getQueueToken()).isEqualTo(existingToken);
        assertThat(response.getQueueNumber()).isEqualTo(42L);

        // then — 재진입이라 이력 저장 디스패치가 발생하지 않아야 함 (이력 중복 방지)
        verify(queueHistoryService, never()).saveWaitingHistoryAsync(
                anyLong(), anyLong(), anyString(), anyLong(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("TC-03: 존재하지 않는 matchId 진입 → MATCH_NOT_FOUND")
    void 존재하지_않는_회차() {
        // given — 게이트 조회 단계에서 MATCH_NOT_FOUND 전파 (실제 발생은 MatchQueryService)
        given(matchQueryService.getBookingGate(99L))
                .willThrow(new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> queueService.enter(99L, 1000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    @DisplayName("TC-04: 예매 오픈 시간 이전 진입 → BOOKING_NOT_OPEN")
    void 예매_오픈_전_진입() {
        // given — 아직 오픈 전 게이트 (bookingOpenAt 이 미래라 isBookableAt=false)
        MatchBookingGate notOpen = new MatchBookingGate(
                EventStatus.OPEN, MatchStatus.SCHEDULED,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        given(matchQueryService.getBookingGate(1L)).willReturn(notOpen);

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

        @Test
        @DisplayName("WAITING + 매진 → soldOut=true, 순번은 유지")
        void waiting_매진() {
            // given — WAITING 상태인데 매진 플래그가 켜진 상황
            Long matchId = 1L;
            String token = "token-soldout";
            given(matchRepository.existsById(matchId)).willReturn(true);
            Map<String, String> meta = new HashMap<>();
            meta.put("matchId", String.valueOf(matchId));
            meta.put("enteredAt", "1700000000000");
            given(queueRedis.getTokenMeta(token)).willReturn(meta);
            given(queueRedis.isAllowed(matchId, token)).willReturn(false);
            given(queueRedis.isWaiting(matchId, token)).willReturn(true);
            given(queueRedis.getRank(matchId, token)).willReturn(50L);
            given(queueRedis.isSoldOut(matchId)).willReturn(true);

            // when
            QueueStatusResponse response = queueService.getStatus(matchId, token);

            // then — 매진이라 soldOut true, 순번(50)은 그대로 노출
            assertThat(response.getStatus()).isEqualTo("WAITING");
            assertThat(response.isSoldOut()).isTrue();
            assertThat(response.getQueueNumber()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("burst 즉시승격 (즉시 ALLOWED 진입)")
    class BurstAdmission {

        @Test
        @DisplayName("TC-14: burst 통과 → ALLOWED 응답 + 이력은 allowed=true 로 디스패치")
        void burst_통과() {
            // given — burst 게이트 ON 으로 전환
            ReflectionTestUtils.setField(queueService, "burstEnabled", true);
            given(matchQueryService.getBookingGate(1L)).willReturn(openGate());

            // Redis 가 burst 통과 시그널 — allowed=true 반환
            given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong(),
                    eq(200), eq(600), eq(true)))
                    .willAnswer(inv -> new QueueRedisRepository.EnterResult(
                            inv.getArgument(2), 1L, true));

            // when
            QueueEnterResponse response = queueService.enter(1L, 1000L);

            // then — 응답이 ALLOWED 로 조립됨
            assertThat(response.getStatus()).isEqualTo("ALLOWED");
            assertThat(response.getAllowedAt()).isNotNull();
            assertThat(response.getEntryDeadline()).isAfter(response.getAllowedAt());

            // then — 이력 디스패치에 allowed=true 전달 (실제 ALLOWED 상태 기록은 QueueHistoryServiceTest 에서 검증)
            verify(queueHistoryService).saveWaitingHistoryAsync(
                    eq(1000L), eq(1L), anyString(), eq(1L), any(), any(), eq(true));
        }

        @Test
        @DisplayName("TC-15: burst 초과 → WAITING 응답 + 이력은 allowed=false 로 디스패치")
        void burst_초과() {
            // given — burst 게이트는 ON 이지만 이미 200 명이 차서 201 번째는 통과 못 함
            ReflectionTestUtils.setField(queueService, "burstEnabled", true);
            given(matchQueryService.getBookingGate(1L)).willReturn(openGate());

            // Redis 가 burst 초과 시그널 — allowed=false, rank=201
            given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong(),
                    eq(200), eq(600), eq(true)))
                    .willAnswer(inv -> new QueueRedisRepository.EnterResult(
                            inv.getArgument(2), 201L, false));

            // when
            QueueEnterResponse response = queueService.enter(1L, 1000L);

            // then — WAITING 응답
            assertThat(response.getStatus()).isEqualTo("WAITING");
            assertThat(response.getQueueNumber()).isEqualTo(201L);
            assertThat(response.getAllowedAt()).isNull();
            assertThat(response.getEntryDeadline()).isNull();

            // then — 이력 디스패치에 allowed=false 전달
            verify(queueHistoryService).saveWaitingHistoryAsync(
                    eq(1000L), eq(1L), anyString(), eq(201L), any(), any(), eq(false));
        }

        @Test
        @DisplayName("TC-16: 재진입 → allowed=false 로 들어와도 이력 저장 스킵")
        void 재진입_burst_무관() {
            // given — burst 게이트 ON, 그러나 재진입 시그널 (Redis 가 기존 토큰 + allowed=false 반환)
            ReflectionTestUtils.setField(queueService, "burstEnabled", true);
            given(matchQueryService.getBookingGate(1L)).willReturn(openGate());

            // 재진입 시그널 — 기존 토큰 그대로 반환 + allowed=false (Repository 가 카운터 안 건드림)
            String existingToken = "existing-token";
            given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong(),
                    eq(200), eq(600), eq(true)))
                    .willReturn(new QueueRedisRepository.EnterResult(existingToken, 42L, false));

            // when
            QueueEnterResponse response = queueService.enter(1L, 1000L);

            // then — 기존 토큰 그대로 + WAITING 응답
            assertThat(response.getQueueToken()).isEqualTo(existingToken);
            assertThat(response.getStatus()).isEqualTo("WAITING");

            // then — 재진입이라 이력 저장 스킵 (이력 중복 방지)
            verify(queueHistoryService, never()).saveWaitingHistoryAsync(
                    anyLong(), anyLong(), anyString(), anyLong(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("TC-17: burstEnabled=false → 첫 진입자도 WAITING (피처 플래그 OFF)")
        void 피처플래그_OFF() {
            // given — burst 게이트 OFF (시연 / 로컬 환경 가정)
            ReflectionTestUtils.setField(queueService, "burstEnabled", false);
            given(matchQueryService.getBookingGate(1L)).willReturn(openGate());

            // burstEnabled=false 가 그대로 Repository 로 전달됨 — 결과는 allowed=false
            given(queueRedis.enter(eq(1L), eq(1000L), anyString(), anyLong(),
                    eq(200), eq(600), eq(false)))
                    .willAnswer(inv -> new QueueRedisRepository.EnterResult(
                            inv.getArgument(2), 1L, false));

            // when
            QueueEnterResponse response = queueService.enter(1L, 1000L);

            // then — 1 명째여도 WAITING 응답 (피처 플래그 OFF 의도)
            assertThat(response.getStatus()).isEqualTo("WAITING");
            assertThat(response.getQueueNumber()).isEqualTo(1L);
            assertThat(response.getAllowedAt()).isNull();
        }
    }
}
