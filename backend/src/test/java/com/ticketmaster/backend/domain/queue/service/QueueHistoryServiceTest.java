package com.ticketmaster.backend.domain.queue.service;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.entity.QueueStatus;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import jakarta.persistence.EntityManager;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 대기열 진입 이력 비동기 저장 서비스 단위 테스트
 *
 * @Async / @Transactional 은 직접 호출 단위 테스트에선 적용 안 되므로(프록시 미경유),
 * 저장 로직 자체만 검증 — getReference 로 FK 참조 / 상태 기록 / 예외 삼킴
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 진입 이력 비동기 저장 서비스 단위 테스트")
class QueueHistoryServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private EntityManager em;

    @InjectMocks
    private QueueHistoryService queueHistoryService;

    @BeforeEach
    void setUp() {
        // em 은 @PersistenceContext 필드 주입이라 @InjectMocks(생성자 주입)로는 안 들어감 → 직접 주입
        ReflectionTestUtils.setField(queueHistoryService, "em", em);
    }

    private static final Long USER_ID = 1000L;
    private static final Long MATCH_ID = 1L;
    private static final String TOKEN = "token-abc";

    @Test
    @DisplayName("TC-01: allowed=false → WAITING 상태로 저장 + getReference 로 FK 참조")
    void 신규_진입_WAITING_저장() {
        // given — getReference 가 SELECT 없이 프록시(목)를 돌려줌
        User userRef = mock(User.class);
        Match matchRef = mock(Match.class);
        given(em.getReference(User.class, USER_ID)).willReturn(userRef);
        given(em.getReference(Match.class, MATCH_ID)).willReturn(matchRef);
        LocalDateTime now = LocalDateTime.now();

        // when
        queueHistoryService.saveWaitingHistoryAsync(
                USER_ID, MATCH_ID, TOKEN, 1L, now, now.plusSeconds(1800), false);

        // then — getReference 로 user/match 참조 (findById 재조회 회피)
        verify(em).getReference(User.class, USER_ID);
        verify(em).getReference(Match.class, MATCH_ID);

        // then — WAITING 상태로 저장, 프록시 참조가 그대로 들어감
        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(queueRepository).save(captor.capture());
        Queue saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(saved.getQueueToken()).isEqualTo(TOKEN);
        assertThat(saved.getQueueNumber()).isEqualTo(1L);
        assertThat(saved.getUser()).isSameAs(userRef);
        assertThat(saved.getMatch()).isSameAs(matchRef);
        assertThat(saved.getAllowedAt()).isNull();
    }

    @Test
    @DisplayName("TC-02: allowed=true → ALLOWED 상태 + allowedAt=enteredAt 로 저장")
    void burst_즉시승격_ALLOWED_저장() {
        // given
        given(em.getReference(User.class, USER_ID)).willReturn(mock(User.class));
        given(em.getReference(Match.class, MATCH_ID)).willReturn(mock(Match.class));
        LocalDateTime enteredAt = LocalDateTime.now();

        // when — allowed=true (burst 즉시승격)
        queueHistoryService.saveWaitingHistoryAsync(
                USER_ID, MATCH_ID, TOKEN, 1L, enteredAt, enteredAt.plusSeconds(1800), true);

        // then — markAllowed 가 적용되어 ALLOWED 상태, allowedAt 은 enteredAt
        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(queueRepository).save(captor.capture());
        Queue saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(QueueStatus.ALLOWED);
        assertThat(saved.getAllowedAt()).isEqualTo(enteredAt);
    }

    @Test
    @DisplayName("TC-03: 저장 중 예외 발생 → 호출자에게 전파하지 않고 삼킴")
    void 저장_실패_예외_삼킴() {
        // given — INSERT 가 실패하는 상황 (DB 장애 등)
        given(em.getReference(User.class, USER_ID)).willReturn(mock(User.class));
        given(em.getReference(Match.class, MATCH_ID)).willReturn(mock(Match.class));
        given(queueRepository.save(any(Queue.class))).willThrow(new RuntimeException("DB down"));
        LocalDateTime now = LocalDateTime.now();

        // when & then — 이력 저장 실패가 사용자 진입(Redis)에 영향 주면 안 되므로 예외를 삼켜야 함
        assertThatCode(() -> queueHistoryService.saveWaitingHistoryAsync(
                USER_ID, MATCH_ID, TOKEN, 1L, now, now.plusSeconds(1800), false))
                .doesNotThrowAnyException();

        // then — 저장 시도 자체는 발생
        verify(queueRepository).save(any(Queue.class));
    }
}
