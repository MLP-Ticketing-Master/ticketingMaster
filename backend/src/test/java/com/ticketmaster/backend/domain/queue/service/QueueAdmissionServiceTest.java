package com.ticketmaster.backend.domain.queue.service;


import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * TK-88 대기열 승격 트랜잭션 단위 테스트
 *
 * Mockito 로 Redis / DB 를 가짜 객체로 대체해서 분기만 빠르게 검증
 * 실제 Redis 동작 검증은 QueueAdmissionIT 에서
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 승격 트랜잭션 단위 테스트")
class QueueAdmissionServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private QueueRedisRepository queueRedis;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private QueueAdmissionService queueAdmissionService;

    @BeforeEach
    void setUp() {
        // @Value 필드는 단위 테스트에선 주입 안 됨 → 직접 세팅
        ReflectionTestUtils.setField(queueAdmissionService, "admissionBatchSize", 200);
        ReflectionTestUtils.setField(queueAdmissionService, "sessionSeconds", 600);
        ReflectionTestUtils.setField(queueAdmissionService, "admissionBuffer", 10);
    }

    @Test
    @DisplayName("정상 승격 → 각 토큰의 DB markAllowed 호출 검증")
    void 정상_승격() {
        // given — 좌석 충분(400)이라 상한 200 으로 승격
        Long matchId = 10L;
        LocalDateTime now = LocalDateTime.now();
        given(seatRepository.countAvailableByMatchId(matchId)).willReturn(400L);
        // Redis 가 3개 토큰을 승격해서 돌려줬다고 가정 (실제는 200개지만 간단히)
        List<String> promoted = List.of("tk1", "tk2", "tk3");
        given(queueRedis.promoteTopN(matchId, 200, 600)).willReturn(promoted);

        // DB 에서 해당 토큰들로 Queue 행을 찾아옴 — mock 객체 반환
        Queue q1 = mock(Queue.class);
        Queue q2 = mock(Queue.class);
        Queue q3 = mock(Queue.class);
        given(queueRepository.findByQueueTokenIn(promoted)).willReturn(List.of(q1, q2, q3));

        // when
        queueAdmissionService.promoteForMatch(matchId, now);

        // then — 각 Queue 의 markAllowed 가 같은 시각으로 1번씩 호출되어야 함
        verify(q1).markAllowed(now);
        verify(q2).markAllowed(now);
        verify(q3).markAllowed(now);
    }

    @Test
    @DisplayName("승격된 토큰 없음 → DB 작업 없이 return")
    void 빈_리스트() {
        // given — 좌석은 있으나 대기열이 비어있어서 ZPOPMIN 결과도 빈 리스트
        Long matchId = 10L;
        given(seatRepository.countAvailableByMatchId(matchId)).willReturn(400L);
        given(queueRedis.promoteTopN(matchId, 200, 600)).willReturn(Collections.emptyList());

        // when
        queueAdmissionService.promoteForMatch(matchId, LocalDateTime.now());

        // then — 빈 리스트면 DB 조회/갱신 자체가 일어나면 안 됨
        verify(queueRepository, never()).findByQueueTokenIn(any());
    }

    @Test
    @DisplayName("DB 갱신 실패 → 로그 + throw (트랜잭션 롤백 확인)")
    void DB_실패() {
        // given — 좌석 충분
        Long matchId = 10L;
        given(seatRepository.countAvailableByMatchId(matchId)).willReturn(400L);
        List<String> promoted = List.of("tk1", "tk2");
        given(queueRedis.promoteTopN(matchId, 200, 600)).willReturn(promoted);
        // DB 조회 단계에서 예외 발생 (DB 다운 등)
        given(queueRepository.findByQueueTokenIn(promoted))
                .willThrow(new RuntimeException("DB connection failed"));

        // when & then — 예외가 그대로 다시 던져져야 @Transactional 이 롤백 트리거
        // 추가로 운영자 보정용 토큰 목록이 로그에 남는데 그건 log.error 라 단위 테스트로는 검증 X
        assertThatThrownBy(() ->
                queueAdmissionService.promoteForMatch(matchId, LocalDateTime.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB connection failed");
    }

    @Test
    @DisplayName("매진(예매 가능 좌석 0) → 매진 플래그 ON + 승격 스킵")
    void 매진_승격중단() {
        // given — 예매 가능 좌석이 0 (전부 SOLD/RESERVED)
        Long matchId = 10L;
        given(seatRepository.countAvailableByMatchId(matchId)).willReturn(0L);

        // when
        queueAdmissionService.promoteForMatch(matchId, LocalDateTime.now());

        // then — 매진 플래그만 켜고 Redis 승격 / DB 조회는 일어나지 않아야 함
        verify(queueRedis).markSoldOut(matchId);
        verify(queueRedis, never()).promoteTopN(anyLong(), anyInt(), anyInt());
        verify(queueRepository, never()).findByQueueTokenIn(any());
    }

    @Test
    @DisplayName("남은 좌석 적으면 좌석 + 버퍼만큼만 승격")
    void 좌석_기반_동적_입장() {
        // given — 남은 좌석 5, 버퍼 10 → 15명만 승격 대상 (상한 200 아님)
        Long matchId = 10L;
        given(seatRepository.countAvailableByMatchId(matchId)).willReturn(5L);
        given(queueRedis.promoteTopN(matchId, 15, 600)).willReturn(List.of("tk1"));
        given(queueRepository.findByQueueTokenIn(List.of("tk1")))
                .willReturn(List.of(mock(Queue.class)));

        // when
        queueAdmissionService.promoteForMatch(matchId, LocalDateTime.now());

        // then — 좌석5 + 버퍼10 = 15 로 승격 (배치 상한 200 으로 안 감)
        verify(queueRedis).promoteTopN(matchId, 15, 600);
    }
}