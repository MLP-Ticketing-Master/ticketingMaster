package com.ticketmaster.backend.domain.booking.concurrency;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사용자 결제 confirm 과 자동 만료 expire 가
 * 동시에 같은 Booking row 를 건드릴 때
 * @Version 이 lost update 를 차단하는지 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // 만료 스케줄러 자동 발화 차단 — 테스트가 수동으로 expire 호출
        "booking.expiry-scan-interval-ms=99999999"
})
@DisplayName("Booking 동시성 통합 테스트 — @Version 낙관적 락")
public class BookingConcurrencyIT {

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
    private BookingRepository bookingRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long bookingId;

    @BeforeEach
    void setUp() {
        // PENDING Booking 시드 — 별도 트랜잭션으로 commit
        bookingId = transactionTemplate.execute(s -> {
            User user = userRepository.save(buildUser());
            Event event = eventRepository.save(buildEvent());
            Match match = matchRepository.save(buildMatch(event));
            Booking booking = Booking.create(user, match,
                    "b-test-" + System.nanoTime(), 50000);
            return bookingRepository.save(booking).getId();
        });
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("confirm 과 expire 동시 호출 → 한 쪽만 성공, 다른 쪽은 OptimisticLock 충돌")
    void confirm_vs_expire() throws Exception {
        // given — PENDING Booking 시드 완료

        // when — 두 스레드가 같은 row 를 동시에 변경
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> race(ready, start, done, successCount, conflictCount,
                () -> transactionTemplate.executeWithoutResult(s ->
                        bookingRepository.findById(bookingId).orElseThrow().confirm())));
        executor.submit(() -> race(ready, start, done, successCount, conflictCount,
                () -> transactionTemplate.executeWithoutResult(s ->
                        bookingRepository.findById(bookingId).orElseThrow().expire())));

        ready.await();
        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // then — 한 쪽만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Booking 최종 상태 — 어느 스레드가 먼저 commit 했는지에 따라 둘 중 하나로 결정
        Booking result = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(result.getStatus()).isIn(BookingStatus.CONFIRMED, BookingStatus.EXPIRED);
    }

    /**
     * 두 스레드 동시 출발 + OptimisticLock 만 conflict 로 집계
     */
    private void race(CountDownLatch ready, CountDownLatch start, CountDownLatch done,
                      AtomicInteger successCount, AtomicInteger conflictCount, Runnable action) {
        try {
            ready.countDown();
            start.await();
            action.run();
            successCount.incrementAndGet();
        } catch (ObjectOptimisticLockingFailureException e) {
            conflictCount.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }finally {
            done.countDown();
        }
    }

    // ------ 테스트 픽스처 ----------------------------------------

    private User buildUser() {
        return User.create(
                "test" + System.nanoTime() + "@test.com",
                "test-encoded-password",
                "test-user",
                null
        );
    }
    private Event buildEvent() {
        return Event.builder()
                .title("동시성-" + System.nanoTime())
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
}
