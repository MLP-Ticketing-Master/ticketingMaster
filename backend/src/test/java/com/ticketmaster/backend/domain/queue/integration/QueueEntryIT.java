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
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * TK-82 대기열 진입 통합 테스트
 *
 * 환경
 *   - Oracle 23c FREE Testcontainer (운영과 동일 DB)
 *   - Redis 7 Testcontainer (GenericContainer 로 간단히 띄움)
 *   - 매 테스트 후 시드 데이터 + Redis 키 청소
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
        "queue.burst-enabled=false"   // 기존 TC-08~13 은 burst OFF 환경 가정 (burst ON 검증은 QueueBurstAdmissionIT 참고)
})
@DisplayName("대기열 진입 통합 테스트")
class QueueEntryIT {

    /**
     * Oracle DB 컨테이너 — Spring Boot 가 자동 연결
     */
    @Container
    @ServiceConnection
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
            .withReuse(true);

    /**
     * Redis 컨테이너 — @ServiceConnection 미지원이라 아래 @DynamicPropertySource 로 직접 주입
     */
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

        // 사용자 시드 — TC-10 의 100명 동시 진입 시나리오를 위해 100명 시드
        userIds = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
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
    @DisplayName("TC-08: 정상 진입 → 토큰 + 순번 1 + DB 이력 저장")
    void 정상_진입() {
        // given — setUp 에서 회차 / 사용자 시드 완료

        // when
        QueueEnterResponse response = queueService.enter(matchId, userIds.get(0));

        // then
        assertThat(response.getQueueToken()).isNotBlank();
        assertThat(response.getQueueNumber()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("WAITING");

        // then — DB 이력 저장 확인 (버퍼 적재분을 직접 flush 해 반영)
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Queue saved = queueRepository.findByQueueToken(response.getQueueToken()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(QueueStatus.WAITING);
            assertThat(saved.getQueueNumber()).isEqualTo(1L);
        });
    }

    @Test
    @DisplayName("TC-09: 5명 순차 진입 → 순번 1~5 정확히 부여")
    void 순차_진입() {
        // given — 5명 시드 완료
        int n = 5;

        // when — 한 명씩 순차 진입
        long[] nums = new long[n];
        for (int i = 0; i < n; i++) {
            nums[i] = queueService.enter(matchId, userIds.get(i)).getQueueNumber();
        }

        // then — 순번 1~5
        for (int i = 0; i < n; i++) {
            assertThat(nums[i]).isEqualTo(i + 1);
        }

        // 이력 INSERT 가 tearDown 청소 전에 모두 반영되도록 직접 flush (다음 테스트 누수 방지)
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).until(() -> queueRepository.count() == n);
    }

    @Test
    @DisplayName("TC-10: 동시 진입 100건 → 모두 성공 + 순번 1~100 중복 없이 부여")
    void 동시_진입_100명() throws Exception {
        // given
        int n = 100;
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        Set<Long> nums = ConcurrentHashMap.newKeySet(); // 중복 없는 동시성 안전 Set
        AtomicInteger failCount = new AtomicInteger();

        // when — 100명이 거의 동시에 진입
        for (int i = 0; i < n; i++) {
            final long uid = userIds.get(i);
            executor.submit(() -> {
                try {
                    ready.countDown();      // 준비 완료
                    start.await();          // 출발 신호 대기
                    long num = queueService.enter(matchId, uid).getQueueNumber();
                    nums.add(num);
                } catch (Throwable t) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();   // 동시 출발
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // then — 100명 모두 성공, 순번 100개가 모두 서로 다르고 1~100 범위 안
        assertThat(failCount.get()).isZero();
        assertThat(nums).hasSize(n);
        assertThat(nums).allMatch(num -> num >= 1L && num <= 100L);

        // 이력 INSERT 100건이 tearDown 청소 전에 모두 반영되도록 직접 flush (다음 테스트 누수 방지)
        queueHistoryService.flush();
        await().atMost(10, TimeUnit.SECONDS).until(() -> queueRepository.count() == n);
    }

    @Test
    @DisplayName("TC-11: 동일 사용자 동시 진입 5건 → 모두 같은 토큰 + DB 이력 1건 (idempotent)")
    void 동일_사용자_동시_진입() throws Exception {
        // given — 같은 사용자가 5번 동시에 진입 시도
        long uid = userIds.get(0);
        int n = 5;
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        Set<String> tokens = ConcurrentHashMap.newKeySet();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();

        // when — 같은 userId 로 5개 스레드가 동시에 enter() 호출
        for (int i = 0; i < n; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    QueueEnterResponse resp = queueService.enter(matchId, uid);
                    tokens.add(resp.getQueueToken());
                } catch (Throwable t) {
                    unexpected.add(t);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // then — SETNX 가 원자적이라 마커 선점은 1번만 성공, 나머지는 기존 토큰을 회수 → 토큰이 단 하나로 수렴
        assertThat(unexpected).isEmpty();
        assertThat(tokens).hasSize(1);

        // then — DB 이력은 신규 진입 1건만 (재진입은 저장 스킵), 직접 flush 해 반영
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).until(() -> queueRepository.count() == 1L);
    }

    @Test
    @DisplayName("TC-12: ALLOWED 권한 살아있을 때 재진입 → 같은 토큰 그대로 + DB 이력 1건")
    void ALLOWED_재진입_같은_토큰() {
        // given — 진입 후 승격까지 진행 (좌석 페이지에서 새로고침하는 시나리오)
        long uid = userIds.get(0);
        QueueEnterResponse first = queueService.enter(matchId, uid);
        String firstToken = first.getQueueToken();
        // 스케줄러 대신 직접 promoteTopN 호출 (1명 승격)
        queueRedis.promoteTopN(matchId, 1, 600);

        // when — 같은 사용자가 다시 enter() 호출
        QueueEnterResponse second = queueService.enter(matchId, uid);

        // then — ALLOWED 권한 살아있어서 기존 토큰 그대로 반환
        assertThat(second.getQueueToken()).isEqualTo(firstToken);

        // then — DB 이력은 1건만 (재진입은 저장 스킵), 직접 flush 해 반영
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).until(() -> queueRepository.count() == 1L);
    }

    @Test
    @DisplayName("TC-13: ALLOWED 권한 만료 후 재진입 → 새 토큰 + DB 이력 2건 (다시 줄서기)")
    void ALLOWED_만료_후_재진입() {
        // given — 진입 → 승격 → ALLOWED 키 강제 만료 (좀비 마커만 남은 상태 시뮬레이션)
        long uid = userIds.get(0);
        QueueEnterResponse first = queueService.enter(matchId, uid);
        String firstToken = first.getQueueToken();
        queueRedis.promoteTopN(matchId, 1, 600);
        // ALLOWED 키 즉시 삭제 — 좌석 못 잡고 10분 만료된 상태 시뮬레이션
        redis.delete("queue:allowed:" + matchId + ":" + firstToken);

        // when — 다시 enter() 호출
        QueueEnterResponse second = queueService.enter(matchId, uid);

        // then — 권한 만료라 새 토큰 발급 + ZSet 비어있어서 1번 자리부터 다시 시작
        assertThat(second.getQueueToken()).isNotEqualTo(firstToken);
        assertThat(second.getQueueNumber()).isEqualTo(1L);

        // then — 신규 진입이라 DB 이력 추가 저장 → 총 2건 (재시도별 누적), 직접 flush 해 반영
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).until(() -> queueRepository.count() == 2L);
    }

    // ─── 시드 빌더 ─────────────────────────────────────────

    private Event buildEvent() {
        return Event.builder()
                .title("대기열 테스트 " + System.nanoTime())
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

    /**
     * 시드용 User 객체
     */
    private User buildUser(int idx) {
        return User.create(
                "queue-" + idx + "-" + System.nanoTime() + "@test.com",
                "dummy-encoded-password",
                "queue-user-" + idx,
                null   // phone 은 optional
        );
    }
}
