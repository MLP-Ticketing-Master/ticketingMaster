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
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

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
        "queue.session-seconds=600",
        "queue.admission-buffer=10",
        "queue.burst-enabled=false"   // 승격 스케줄러 동작 검증이라 burst 차단 (첫 200 명이 자동 통과되면 부분승격 전제 깨짐)
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
    @Autowired private com.ticketmaster.backend.domain.queue.service.QueueHistoryService queueHistoryService;
    @Autowired private QueueAdmissionService queueAdmissionService;
    @Autowired private QueueTokenValidator queueTokenValidator;
    @Autowired private QueueRepository queueRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private SeatGradeRepository seatGradeRepository;
    @Autowired private StringRedisTemplate redis;

    private Long matchId;
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        // 이벤트 + 매치 생성 (예매 가능 시간 안)
        Event event = eventRepository.save(buildEvent());
        Match match = matchRepository.save(buildMatch(event));
        matchId = match.getId();

        // AVAILABLE 좌석 200개 시드 — 좌석 기반 동적 입장이 작동하도록 (좌석 없으면 매진 처리로 승격 0)
        seedSeats(event, match, 200);

        // 500명 시드
        userIds = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            User user = userRepository.save(buildUser(i));
            userIds.add(user.getId());
        }
    }

    @AfterEach
    void tearDown() {
        // FK 순서 — queue/seat 먼저, 그다음 section/grade, 마지막에 match/event
        queueRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        // Redis 모든 키 청소 (테스트 격리)
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("대기열 500명 → 스케줄러 1회 → 상위 200명 ALLOWED + 나머지 300명 WAITING")
    void 부분승격_500명() {
        // given — 500명 순차 진입
        for (int i = 0; i < 500; i++) {
            queueService.enter(matchId, userIds.get(i));
        }
        // 승격이 DB row 를 찾으려면 enter 이력 500건이 먼저 반영돼야 함 → 직접 flush
        queueHistoryService.flush();
        await().atMost(15, TimeUnit.SECONDS).until(() -> queueRepository.count() == 500L);

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
    @DisplayName("활성 매치 2개 → 매치별로 각각 처리")
    void 여러_매치() {
        // given — 두 번째 매치 추가 (좌석도 각각 시드), 각각 100명씩 진입
        Event event2 = eventRepository.save(buildEvent());
        Match match2 = matchRepository.save(buildMatch(event2));
        Long matchId2 = match2.getId();
        seedSeats(event2, match2, 200);

        for (int i = 0; i < 100; i++) {
            queueService.enter(matchId, userIds.get(i));
            queueService.enter(matchId2, userIds.get(i + 100));
        }
        // 두 매치 합산 200건이 먼저 반영돼야 승격 가능 → 직접 flush
        queueHistoryService.flush();
        await().atMost(15, TimeUnit.SECONDS).until(() -> queueRepository.count() == 200L);

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
    @DisplayName("enter → 폴링 → 승격 → 좌석 점유 권한 검증")
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

        // 승격이 DB row 를 ALLOWED 로 갱신하려면 enter 이력이 먼저 반영돼야 함 → 직접 flush
        queueHistoryService.flush();
        await().atMost(5, TimeUnit.SECONDS).until(() -> queueRepository.count() == 1L);

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
        return Event.builder()
                .title("승격 통합 " + System.nanoTime())
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

    /** 매치에 AVAILABLE 좌석 시드 — 좌석 기반 동적 입장이 작동하도록 (Section/SeatGrade 동반 생성) */
    private void seedSeats(Event event, Match match, int count) {
        Section section = sectionRepository.save(Section.create(event, "A구역", 1, "A구역"));
        SeatGrade grade = seatGradeRepository.save(SeatGrade.create(event, "VIP", 100000, "#FF0000"));
        List<Seat> seats = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String rowLabel = String.valueOf((char) ('A' + i / 10));
            int seatNo = i % 10 + 1;
            seats.add(Seat.create(match, section, grade, rowLabel, seatNo, "S-" + i));
        }
        seatRepository.saveAll(seats);
    }
}
