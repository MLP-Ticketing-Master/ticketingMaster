package com.ticketmaster.backend.domain.queue.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 대기열 진입 이력 버퍼/스케줄러 서비스 단위 테스트
 *
 * 실제 저장(getReference/saveAll)은 QueueHistoryWriter 로 분리됐으므로 writer 를 목으로 대체
 * 여기선 버퍼 적재 / flush drain / 상한 초과 즉시 flush / flushMax 제한 / 예외 삼킴 / 종료 flush 만 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 진입 이력 버퍼/스케줄러 서비스 단위 테스트")
class QueueHistoryServiceTest {

    @Mock
    private QueueHistoryWriter writer;

    @InjectMocks
    private QueueHistoryService queueHistoryService;

    @BeforeEach
    void setUp() {
        // @Value 는 단위 테스트에서 주입되지 않으므로 ReflectionTestUtils 로 직접 세팅
        ReflectionTestUtils.setField(queueHistoryService, "bufferMax", 20000);
        ReflectionTestUtils.setField(queueHistoryService, "flushMax", 2000);
    }

    private static final Long USER_ID = 1000L;
    private static final Long MATCH_ID = 1L;

    private QueueHistoryRecord record(long queueNumber) {
        LocalDateTime now = LocalDateTime.now();
        return new QueueHistoryRecord(
                USER_ID, MATCH_ID, "token-" + queueNumber, queueNumber, now, now.plusSeconds(1800), false);
    }

    @Test
    @DisplayName("enqueue → 버퍼 적재만 하고 즉시 저장하지 않음")
    void 적재만_하고_저장은_안함() {
        // given
        // when — 상한 한참 아래로 1건만 적재
        queueHistoryService.enqueueWaitingHistory(record(1L));

        // then — 버퍼에만 쌓이고 writer 는 호출되지 않음
        verify(writer, never()).saveBatch(any());
    }

    @Test
    @DisplayName("flush → 적재분을 모아 writer.saveBatch 로 한 번에 위임")
    void flush_적재분_위임() {
        // given — 3건 적재
        queueHistoryService.enqueueWaitingHistory(record(1L));
        queueHistoryService.enqueueWaitingHistory(record(2L));
        queueHistoryService.enqueueWaitingHistory(record(3L));

        // when
        queueHistoryService.flush();

        // then — 3건이 한 배치로 writer 에 전달됨
        ArgumentCaptor<List<QueueHistoryRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(writer).saveBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("빈 버퍼 flush → writer 호출 안 함")
    void 빈_버퍼_flush_무동작() {
        // given — 적재 없음
        // when
        queueHistoryService.flush();

        // then
        verify(writer, never()).saveBatch(any());
    }

    @Test
    @DisplayName("버퍼 상한 초과 → 적재 시점에 즉시 flush 발생")
    void 상한_초과_즉시_flush() {
        // given — 상한을 2로 낮춤
        ReflectionTestUtils.setField(queueHistoryService, "bufferMax", 2);

        // when — 2건째 적재에서 pending 이 상한에 도달해 즉시 flush
        queueHistoryService.enqueueWaitingHistory(record(1L));
        queueHistoryService.enqueueWaitingHistory(record(2L));

        // then — 즉시 flush 로 writer 가 호출됨
        verify(writer).saveBatch(any());
    }

    @Test
    @DisplayName("flushMax → 한 번에 그 건수까지만 비우고 나머지는 버퍼에 남김")
    void flushMax_제한() {
        // given — 한 번에 2건까지만 비우도록 제한, 3건 적재
        ReflectionTestUtils.setField(queueHistoryService, "flushMax", 2);
        queueHistoryService.enqueueWaitingHistory(record(1L));
        queueHistoryService.enqueueWaitingHistory(record(2L));
        queueHistoryService.enqueueWaitingHistory(record(3L));

        // when — 첫 flush 는 2건, 둘째 flush 는 남은 1건
        queueHistoryService.flush();
        queueHistoryService.flush();

        // then — 배치 크기가 2 → 1 순으로 위임됨
        ArgumentCaptor<List<QueueHistoryRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(writer, times(2)).saveBatch(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasSize(2);
        assertThat(captor.getAllValues().get(1)).hasSize(1);
    }

    @Test
    @DisplayName("writer 저장 실패 → 예외를 삼켜 진입(Redis)에 영향 주지 않음")
    void 저장_실패_예외_삼킴() {
        // given — writer 가 저장 중 예외 발생
        willThrow(new RuntimeException("DB down")).given(writer).saveBatch(any());
        queueHistoryService.enqueueWaitingHistory(record(1L));

        // when & then — flush 가 예외를 밖으로 던지지 않아야 함
        assertThatCode(() -> queueHistoryService.flush()).doesNotThrowAnyException();
        verify(writer).saveBatch(any());
    }

    @Test
    @DisplayName("종료 시 flush → 버퍼가 빌 때까지 모두 비움")
    void 종료시_잔여분_flush() {
        // given — flushMax 보다 많은 건수가 남아있어도 종료 시 전부 비워야 함
        ReflectionTestUtils.setField(queueHistoryService, "flushMax", 2);
        queueHistoryService.enqueueWaitingHistory(record(1L));
        queueHistoryService.enqueueWaitingHistory(record(2L));
        queueHistoryService.enqueueWaitingHistory(record(3L));

        // when
        queueHistoryService.flushOnShutdown();

        // then — 2건 + 1건으로 두 번에 걸쳐 모두 위임됨
        verify(writer, times(2)).saveBatch(any());
    }
}
