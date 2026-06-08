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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 대기열 진입 이력 배치 저장 Writer 단위 테스트
 *
 * @Transactional 은 직접 호출 단위 테스트에선 적용 안 되므로(프록시 미경유) 저장 로직 자체만 검증
 * getReference 로 FK 참조(재조회 회피) / 상태 기록(WAITING·ALLOWED) / saveAll 위임 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 진입 이력 배치 저장 Writer 단위 테스트")
class QueueHistoryWriterTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private EntityManager em;

    @InjectMocks
    private QueueHistoryWriter writer;

    @BeforeEach
    void setUp() {
        // em 은 @PersistenceContext 필드 주입이라 @InjectMocks(생성자 주입)로는 안 들어감 → 직접 주입
        ReflectionTestUtils.setField(writer, "em", em);
    }

    private static final Long USER_ID = 1000L;
    private static final Long MATCH_ID = 1L;
    private static final String TOKEN = "token-abc";

    private QueueHistoryRecord record(LocalDateTime enteredAt, boolean allowed) {
        return new QueueHistoryRecord(
                USER_ID, MATCH_ID, TOKEN, 1L, enteredAt, enteredAt.plusSeconds(1800), allowed);
    }

    @Test
    @DisplayName("allowed=false → WAITING 상태로 저장 + getReference 로 FK 참조")
    void 신규_진입_WAITING_저장() {
        // given — getReference 가 SELECT 없이 프록시(목)를 돌려줌
        User userRef = mock(User.class);
        Match matchRef = mock(Match.class);
        given(em.getReference(User.class, USER_ID)).willReturn(userRef);
        given(em.getReference(Match.class, MATCH_ID)).willReturn(matchRef);
        LocalDateTime now = LocalDateTime.now();

        // when
        writer.saveBatch(List.of(record(now, false)));

        // then — getReference 로 user/match 참조 (findById 재조회 회피)
        verify(em).getReference(User.class, USER_ID);
        verify(em).getReference(Match.class, MATCH_ID);

        // then — WAITING 상태로 저장, 프록시 참조가 그대로 들어감
        ArgumentCaptor<List<Queue>> captor = ArgumentCaptor.forClass(List.class);
        verify(queueRepository).saveAll(captor.capture());
        Queue saved = captor.getValue().get(0);
        assertThat(saved.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(saved.getQueueToken()).isEqualTo(TOKEN);
        assertThat(saved.getQueueNumber()).isEqualTo(1L);
        assertThat(saved.getUser()).isSameAs(userRef);
        assertThat(saved.getMatch()).isSameAs(matchRef);
        assertThat(saved.getAllowedAt()).isNull();
    }

    @Test
    @DisplayName("allowed=true → ALLOWED 상태 + allowedAt=enteredAt 로 저장")
    void burst_즉시승격_ALLOWED_저장() {
        // given
        given(em.getReference(User.class, USER_ID)).willReturn(mock(User.class));
        given(em.getReference(Match.class, MATCH_ID)).willReturn(mock(Match.class));
        LocalDateTime enteredAt = LocalDateTime.now();

        // when — allowed=true (burst 즉시승격)
        writer.saveBatch(List.of(record(enteredAt, true)));

        // then — markAllowed 가 적용되어 ALLOWED 상태, allowedAt 은 enteredAt
        ArgumentCaptor<List<Queue>> captor = ArgumentCaptor.forClass(List.class);
        verify(queueRepository).saveAll(captor.capture());
        Queue saved = captor.getValue().get(0);
        assertThat(saved.getStatus()).isEqualTo(QueueStatus.ALLOWED);
        assertThat(saved.getAllowedAt()).isEqualTo(enteredAt);
    }

    @Test
    @DisplayName("여러 건 → record 수만큼 엔티티 생성해 saveAll 한 번으로 위임")
    void 여러건_배치_저장() {
        // given — 3건, 각기 다른 token/번호
        given(em.getReference(User.class, USER_ID)).willReturn(mock(User.class));
        given(em.getReference(Match.class, MATCH_ID)).willReturn(mock(Match.class));
        LocalDateTime now = LocalDateTime.now();
        List<QueueHistoryRecord> batch = List.of(
                new QueueHistoryRecord(USER_ID, MATCH_ID, "t1", 1L, now, now.plusSeconds(1800), false),
                new QueueHistoryRecord(USER_ID, MATCH_ID, "t2", 2L, now, now.plusSeconds(1800), false),
                new QueueHistoryRecord(USER_ID, MATCH_ID, "t3", 3L, now, now.plusSeconds(1800), false));

        // when
        writer.saveBatch(batch);

        // then — 3건이 한 번의 saveAll 로 전달됨
        ArgumentCaptor<List<Queue>> captor = ArgumentCaptor.forClass(List.class);
        verify(queueRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }
}
