package com.ticketmaster.backend.domain.queue.integration;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.dto.response.QueueStatusResponse;
import com.ticketmaster.backend.domain.queue.entity.QueueStatus;
import com.ticketmaster.backend.domain.queue.repository.QueueRepository;
import com.ticketmaster.backend.domain.queue.service.QueueAdmissionService;
import com.ticketmaster.backend.domain.queue.service.QueueService;
import com.ticketmaster.backend.domain.queue.util.QueueTokenValidator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TK-88 대기열 승격 통합 테스트
 *
 * 환경
 *   - Oracle 23c FREE Testcontainer (운영과 동일 DB)
 *   - Redis 7 Testcontainer
 *   - 매 테스트 후 시드 + Redis 청소
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
        "queue.session-seconds=600"
})
@DisplayName("대기열 승격 통합 테스트")
public class QueueAdmissionIT {

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
    @Autowired private QueueAdmissionService queueAdmissionService;
    @Autowired private QueueTokenValidator queueTokenValidator;
    @Autowired private QueueRepository queueRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StringRedisTemplate redis;

    private Long matchId;
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        // 이벤트 + 매치 생성 (예매 가능 시간 안)
        Event event = eventRepository.save(buildEvent());
        Match match = matchRepository.save(buildMatch(event));
        matchId = match.getId();

        // 500명 시드
        userIds = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
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
        // Redis 모든 키 청소 (테스트 격리)
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("TC-17: 대기열 500명 → 스케줄러 1회 → 상위 200명 ALLOWED + 나머지 300명 WAITING")
    void 부분승격_500명() {
        // given — 500명 순차 진입
        for (int i = 0; i < 500; i++) {
            queueService.enter(matchId, userIds.get(i));
        }

        // when — 스케줄러 직접 호출
        queueAdmissionService.promoteForMatch(matchId, LocalDateTime.now());

        // then — 상위 200 명만 ALLOWED, 나머지 300명 WAITING
        long allowedCount = queueRepository.findAll().stream()
                .filter(q -> q.getStatus() == QueueStatus.ALLOWED)
                .count();
        long waitingCount = queueRepository.findAll().stream()
                .filter(q -> q.getStatus() == QueueStatus.WAITING)
                .count();
        assertThat(allowedCount).isEqualTo(200);
        assertThat(waitingCount).isEqualTo(300);
    }

    @Test
    @DisplayName("TC-18: 활성 매치 2개 → 매치별로 각각 처리")
    void 여러_매치() {
        // given — 두 번째 매치 추가, 각각 100명씩 진입
        Event event2 = eventRepository.save(buildEvent());
        Match match2 = matchRepository.save(buildMatch(event2));
        Long matchId2 = match2.getId();

        for (int i = 0; i < 100; i++) {
            queueService.enter(matchId, userIds.get(i));
            queueService.enter(matchId2, userIds.get(i + 100));
        }

        // when — 두 매치 모두 승격
        LocalDateTime now = LocalDateTime.now();
        queueAdmissionService.promoteForMatch(matchId, now);
        queueAdmissionService.promoteForMatch(matchId2, now);

        // then — 매치 별로 각자 100명씩 ALLOWED (200명 batch 안 채워도 다 승격)
        long match1Allowed = queueRepository.findAll().stream()
                .filter(q -> q.getMatch().getId().equals(matchId)
                        && q.getStatus() == QueueStatus.ALLOWED)
                .count();
        long match2Allowed = queueRepository.findAll().stream()
                .filter(q -> q.getMatch().getId().equals(matchId2)
                        && q.getStatus() == QueueStatus.ALLOWED)
                .count();
        assertThat(match1Allowed).isEqualTo(100);
        assertThat(match2Allowed).isEqualTo(100);
    }

    @Test
    @DisplayName("TC-19: enter → 폴링 → 승격 → 좌석 점유 권한 검증")
    void 대기열_진입부터_좌석_점유_권한_획득까지() {
        // given — 한 사용자
        Long userId = userIds.get(0);

        // 1) 큐 진입
        QueueEnterResponse enter = queueService.enter(matchId, userId);
        String token = enter.getQueueToken();
        assertThat(token).isNotBlank();
        assertThat(enter.getQueueNumber()).isEqualTo(1L);

        // 2) 폴링 — 처음엔 WAITING
        QueueStatusResponse status1 = queueService.getStatus(matchId, token);
        assertThat(status1.getStatus()).isEqualTo("WAITING");

        // 3) 스케줄러 승격
        queueAdmissionService.promoteForMatch(matchId, LocalDateTime.now());

        // 4) 폴링 — ALLOWED 됐는지 확인
        QueueStatusResponse status2 = queueService.getStatus(matchId, token);
        assertThat(status2.getStatus()).isEqualTo("ALLOWED");
        assertThat(status2.getAllowedAt()).isNotNull();
        assertThat(status2.getEntryDeadline()).isNotNull();

        // 5) 좌석 점유 권한 검증 — 예외 없이 통과해야 함
        assertThatCode(() -> queueTokenValidator.validateAllowed(matchId, token))
                .doesNotThrowAnyException();
    }

    // ─── 시드 빌더 ─────────────────────────────────────

    private Event buildEvent() {
        LocalDateTime now = LocalDateTime.now();
        return Event.builder()
                .title("승격 통합 " + System.nanoTime())
                .sportType(SportType.LOL)
                .place("테스트 경기장")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .bookingOpenAt(now.minusDays(1))   // 이미 오픈됨
                .bookingCloseAt(now.plusDays(7))
                .maxTicketsPerUser(2)
                .cancelFee(0)
                .status(EventStatus.OPEN)
                .build();
    }

    private Match buildMatch(Event event) {
        return Match.builder()
                .event(event)
                .roundLabel("1R")
                .matchDate(LocalDate.now().plusDays(1))
                .startAt(LocalDateTime.now().plusDays(1))
                .status(MatchStatus.SCHEDULED)
                .build();
    }

    private User buildUser(int idx) {
        return User.create(
                "admit-" + idx + "-" + System.nanoTime() + "@test.com",
                "dummy-encoded-password",
                "admit-user-" + idx,
                null
        );
    }
}
