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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * burst 게이트 ON 환경 통합 테스트
 * QueueEntryIT 와 분리한 이유 — @TestPropertySource 는 클래스 단위 고정이라
 *                              burst ON / OFF 를 한 IT 에서 함께 검증 불가
 *
 * 환경
 *   - Oracle 23c FREE Testcontainer
 *   - Redis 7 Testcontainer
 *   - queue.burst-enabled=true (피처 플래그 ON)
 *   - 사용자 250 명 시드 (TC-20 의 동시 진입 250 건 시나리오용)
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
        "queue.burst-enabled=true"   // 핵심 — 피처 플래그 ON
})
@DisplayName("대기열 진입 통합 테스트 (burst 게이트 ON)")
class QueueBurstAdmissionIT {

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
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        Event event = eventRepository.save(buildEvent());
        Match match = matchRepository.save(buildMatch(event));
        matchId = match.getId();

        // 250 명 시드 — TC-20 의 200 초과 진입 시나리오용
        userIds = new ArrayList<>(250);
        for (int i = 0; i < 250; i++) {
            User user = userRepository.save(buildUser(i));
            userIds.add(user.getId());
        }
    }

    @AfterEach
    void tearDown() {
        queueRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        // Redis 의 모든 키 청소 (테스트 격리)
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("TC-18: burst 첫 진입자 → 즉시 ALLOWED + ZSet 빠짐 + allowed 키 생성 + Hash status=ALLOWED")
    void burst_첫_진입자_상태_검증() {
        // given — setUp 에서 시드 완료, burstEnabled=true

        // when — 첫 진입
        QueueEnterResponse response = queueService.enter(matchId, userIds.get(0));

        // then — 응답이 ALLOWED 상태
        assertThat(response.getStatus()).isEqualTo("ALLOWED");
        assertThat(response.getAllowedAt()).isNotNull();

        String token = response.getQueueToken();

        // then — ZSet 에서 빠져있음 (applyAllowed 가 제거)
        assertThat(queueRedis.isWaiting(matchId, token)).isFalse();

        // then — allowed 키 생성됨
        assertThat(queueRedis.isAllowed(matchId, token)).isTrue();

        // then — Hash 의 status 가 ALLOWED 로 갱신됨
        Map<String, String> meta = queueRedis.getTokenMeta(token);
        assertThat(meta.get("status")).isEqualTo("ALLOWED");
        assertThat(meta.get("allowedAt")).isNotNull();

        // then — DB Queue 도 ALLOWED 상태로 저장
        // 버퍼 적재분을 테스트 스레드에서 직접 flush 해 반영 (백그라운드 스케줄러와의 레이스 회피)
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Queue saved = queueRepository.findByQueueToken(token).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(QueueStatus.ALLOWED);
        });
    }

    @Test
    @DisplayName("TC-19: burst 키 TTL = token-ttl-seconds 로 설정됨")
    void burst_키_TTL_검증() {
        // given — 첫 진입자 1 명

        // when
        queueService.enter(matchId, userIds.get(0));

        // then — burst 키가 tokenTtlSeconds (1800) 와 동일한 TTL 로 설정됨
        String burstKey = "queue:burst:" + matchId;
        Long ttl = redis.getExpire(burstKey, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(1700L, 1800L);   // 호출 사이 약간의 시차 허용

        // 이력 INSERT 가 tearDown 청소 전에 반영되도록 직접 flush (다음 테스트 누수 방지)
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).until(() -> queueRepository.count() == 1L);
    }

    @Test
    @DisplayName("TC-20: 동시 진입 250 건 → 정확히 admissionBatchSize(200) 명만 ALLOWED")
    void burst_동시_진입_정확히_200() throws Exception {
        // given — 250 명 시드 (setUp), 한도 200
        int n = 250;
        int limit = 200;
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger waitingCount = new AtomicInteger();
        // 토큰 중복 검증용 (선택)
        ConcurrentHashMap.KeySetView<String, Boolean> tokens = ConcurrentHashMap.newKeySet();

        // when — 250 명이 거의 동시에 진입
        for (int i = 0; i < n; i++) {
            final long uid = userIds.get(i);
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    QueueEnterResponse resp = queueService.enter(matchId, uid);
                    tokens.add(resp.getQueueToken());
                    if ("ALLOWED".equals(resp.getStatus())) {
                        allowedCount.incrementAndGet();
                    } else {
                        waitingCount.incrementAndGet();
                    }
                } catch (Throwable ignored) {
                    // 실패는 카운터로 안 잡고 done 만 내림
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();   // 동시 출발
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // then — 정확히 200 명만 ALLOWED, 나머지 50 명은 WAITING
        assertThat(allowedCount.get()).isEqualTo(limit);
        assertThat(waitingCount.get()).isEqualTo(n - limit);

        // then — 토큰은 250 개 전부 서로 다름 (idempotent 재진입 아님)
        assertThat(tokens).hasSize(n);

        // then — Redis 카운터도 정확히 200 (INCR/DECR 보정 검증)
        String burstKey = "queue:burst:" + matchId;
        assertThat(Integer.parseInt(redis.opsForValue().get(burstKey))).isEqualTo(limit);

        // 이력 INSERT 250건이 tearDown 청소 전에 모두 반영되도록 직접 flush (다음 테스트 누수 방지)
        queueHistoryService.flush();
        await().atMost(10, TimeUnit.SECONDS).until(() -> queueRepository.count() == n);
    }

    // ─── 시드 빌더 ─────────────────────────────────────────

    private Event buildEvent() {
        return Event.builder()
                .title("burst-on 테스트 " + System.nanoTime())
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
                .bookingOpenAt(now.minusDays(1))   // 이미 오픈됨
                .bookingCloseAt(now.plusDays(7))
                .build();
    }

    private User buildUser(int idx) {
        return User.create(
                "burst-on-" + idx + "-" + System.nanoTime() + "@test.com",
                "dummy-encoded-password",
                "burst-on-user-" + idx,
                null
        );
    }
}
