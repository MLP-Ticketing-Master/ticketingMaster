package com.ticketmaster.backend.domain.queue.integration;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.entity.Queue;
import com.ticketmaster.backend.domain.queue.entity.QueueStatus;
import com.ticketmaster.backend.domain.queue.repository.QueueRedisRepository;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.queue.service.QueueService;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * burst 게이트 OFF 환경 통합 테스트
 * QueueEntryIT 와 분리한 이유 — @TestPropertySource 는 클래스 단위 고정이라
 *                              burst ON / OFF 를 한 IT 에서 함께 검증 불가
 *
 * 환경
 *   - Oracle 23c FREE Testcontainer
 *   - Redis 7 Testcontainer
 *   - queue.burst-enabled=false (피처 플래그 OFF — 시연 / 로컬 환경 가정)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "seat.reservation-ttl-seconds=420",
        "seat.expiry-scan-interval-ms=30000",
        "queue.token-ttl-seconds=1800",
        "queue.admission-batch-size=200",
        "queue.admission-interval-seconds=30",
        "queue.session-seconds=600",
        "queue.burst-enabled=false"   // 핵심 — 피처 플래그 OFF
})
@DisplayName("대기열 진입 통합 테스트 (burst 게이트 OFF)")
class QueueBurstDisabledIT {

    @Container
    @ServiceConnection
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
            .withReuse(true);

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private QueueService queueService;

    @Autowired
    private com.ticketmaster.backend.domain.queue.service.QueueHistoryService queueHistoryService;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private QueueRedisRepository queueRedis;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    private Long matchId;
    private Long userId;

    @BeforeEach
    void setUp() {
        Event event = eventRepository.save(buildEvent());
        Match match = matchRepository.save(buildMatch(event));
        matchId = match.getId();
        User user = userRepository.save(buildUser());
        userId = user.getId();
    }

    @AfterEach
    void tearDown() {
        queueRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("TC-21: 피처 플래그 OFF → 첫 진입자도 WAITING (즉시승격 차단)")
    void burst_피처플래그_OFF() {
        // given — burst-enabled=false 환경 (시연 / 로컬 가정)

        // when — 첫 진입자
        QueueEnterResponse response = queueService.enter(matchId, userId);

        // then — 1 명째여도 WAITING 응답
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getQueueNumber()).isEqualTo(1L);
        assertThat(response.getAllowedAt()).isNull();
        assertThat(response.getEntryDeadline()).isNull();

        // then — DB Queue 도 WAITING 상태로 저장 (버퍼 적재분을 직접 flush 해 반영)
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Queue saved = queueRepository.findByQueueToken(response.getQueueToken()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(QueueStatus.WAITING);
        });

        // then — Redis ZSet 의 WAITING 명단에 토큰이 들어있음
        assertThat(queueRedis.isWaiting(matchId, response.getQueueToken())).isTrue();

        // then — burst 카운터 키 자체가 만들어지지 않음 (게이트 자체를 안 거쳐서)
        String burstKey = "queue:burst:" + matchId;
        assertThat(redis.hasKey(burstKey)).isFalse();
    }

    // ─── 시드 빌더 ─────────────────────────────────────────

    private Event buildEvent() {
        return Event.builder()
                .title("burst-off 테스트 " + System.nanoTime())
                .sportType(SportType.LOL)
                .place("테스트 경기장")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .maxTicketsPerUser(2)
                .cancelFee(0)
                .status(EventStatus.OPEN)
                .build();
    }

    private Match buildMatch(Event event) {
        LocalDateTime now = LocalDateTime.now();
        return Match.builder()
                .event(event)
                .roundLabel("1R")
                .matchDate(LocalDate.now().plusDays(1))
                .startAt(now.plusDays(1))
                .bookingOpenAt(now.minusDays(1))
                .bookingCloseAt(now.plusDays(7))
                .build();
    }

    private User buildUser() {
        return User.create(
                "burst-off-" + System.nanoTime() + "@test.com",
                "dummy-encoded-password",
                "burst-off-user",
                null
        );
    }
}
