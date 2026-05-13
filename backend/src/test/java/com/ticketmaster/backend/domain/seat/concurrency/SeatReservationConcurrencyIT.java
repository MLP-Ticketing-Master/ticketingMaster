package com.ticketmaster.backend.domain.seat.concurrency;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
import com.ticketmaster.backend.domain.seat.service.SeatReservationService;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좌석 점유 동시성 통합 테스트
 *
 * 목적
 *   - SeatReservationService 의 낙관적 락(@Version) + 재시도 로직이
 *     실제 DB 환경에서 멀티 스레드로 정상 동작하는지 검증
 *
 * 환경
 *   - Oracle 23c FREE Testcontainers 로 운영과 동일한 DB 띄움
 *   - @ServiceConnection 으로 datasource 자동 주입 (yaml 의 localhost:1522 설정 무시됨)
 *   - ddl-auto=create-drop, @AfterEach 로 매 테스트 후 데이터 정리
 *
 * 트랜잭션 정책
 *   - 클래스에 @Transactional 사용 안 함
 *     → 각 스레드가 별개 트랜잭션으로 커밋해야 @Version 충돌이 발생함
 *     → 메서드 단위 롤백을 켜면 충돌 자체가 안 일어나 테스트가 의미 없어짐
 *   - 대신 @AfterEach 에서 직접 deleteAllInBatch
 *
 * 시나리오
 *   - TC-01: 동일 좌석 2명 동시 점유 → 1명 성공, 1명 SEAT_ALREADY_RESERVED
 *   - TC-02: 다른 좌석 10명 동시 점유 → 모두 성공
 *   - TC-03: 동일 좌석 20명 동시 점유 → 정확히 1명만 성공 (좌석 중복 0건)
 *   - TC-04: 묶음 점유 부분 충돌 — A=[s1,s2], B=[s2,s3] → 한 명만 성공
 *
 * 미포함
 *   - BookingSeat UNIQUE 위반 → SEAT_ALREADY_RESERVED 변환
 *     : RESERVED→SOLD 처리하는 BookingService 가 아직 없음
 *       BookingService 구현 티켓에서 별도 통합 테스트로 검증 예정
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        // 점유 TTL — 운영 yaml(420)과 동일
        "seat.reservation-ttl-seconds=420",
        // SeatExpiryScheduler 빈 생성용 — 테스트 중 발화는 안 함
        "seat.expiry-scan-interval-ms=30000",
        // 컨텍스트 시작 시 스키마 생성, 종료 시 drop (테스트 후 흔적 안 남김)
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("좌석 점유 동시성 통합 테스트 — 낙관적 락 + 재시도")
class SeatReservationConcurrencyIT {

    /**
     * Oracle 23c FREE 컨테이너 (운영과 동일 dialect / 시퀀스)
     * - 첫 실행 시 이미지 pull(~1~2분), 이후 캐시
     * - withReuse(true) + ~/.testcontainers.properties 의 reuse 설정 시
     * 테스트 종료 후에도 컨테이너 살려둠 → 다음 실행이 즉시
     */
    @Container
    @ServiceConnection
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
            .withReuse(true);

    @Autowired private SeatReservationService seatReservationService;
    @Autowired private SeatRepository seatRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private SeatGradeRepository seatGradeRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private EventRepository eventRepository;

    /** 매 테스트마다 새로 시드되는 회차 ID */
    private Long matchId;
    /** 매 테스트마다 새로 시드되는 좌석 ID 목록 (총 SEAT_COUNT 개, 모두 AVAILABLE) */
    private List<Long> seatIds;

    /** 시드 좌석 수 — 다중 사용자/좌석 시나리오에 충분하도록 여유 있게 */
    private static final int SEAT_COUNT = 30;
    /** Event.maxTicketsPerUser — 묶음 점유 검증 시 사용 */
    private static final int MAX_TICKETS_PER_USER = 2;

    @BeforeEach
    void setUp() {
        // Event → Match → Section → SeatGrade → Seat 순으로 시드
        Event event = eventRepository.save(buildEvent());
        Match match = matchRepository.save(buildMatch(event));
        Section section = sectionRepository.save(Section.create(event, "좌측", 1, null));
        SeatGrade vip = seatGradeRepository.save(
                SeatGrade.create(event, "VIP", 100000, "#FFD700"));

        List<Seat> seats = new ArrayList<>(SEAT_COUNT);
        for (int i = 1; i <= SEAT_COUNT; i++) {
            seats.add(Seat.create(match, section, vip, "A", i, "VIP-A-" + i));
        }
        seatRepository.saveAll(seats);

        matchId = match.getId();
        seatIds = seats.stream().map(Seat::getId).toList();
    }

    @AfterEach
    void tearDown() {
        // 동시성 테스트는 @Transactional 롤백을 못 씀
        // → 매 테스트가 끝나면 시드 데이터를 직접 정리
        // FK 의존 순서: Seat → SeatGrade/Section/Match → Event
        seatRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
    }

    // ─────────────────────────────────────────────────────────────
    // 동일 좌석 동시 점유
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("동일 좌석 동시 점유")
    class SameSeat {

        @Test
        @DisplayName("TC-01: 같은 좌석 1개를 2명이 동시 점유 → 정확히 1명만 성공")
        void 동일좌석_2명_동시() throws Exception {
            // given — 첫 번째 좌석을 타깃으로
            Long target = seatIds.get(0);
            int threadCount = 2;

            // when — 두 스레드가 같은 좌석을 동시에 점유 시도 (idx 는 안 씀)
            ConcurrentResult result = runConcurrently(threadCount,
                    (userId, idx) -> seatReservationService.reserve(matchId, userId, List.of(target)));

            // then — 1명 성공, 나머지는 SEAT_ALREADY_RESERVED
            assertThat(result.unexpected).isEmpty();
            assertThat(result.successCount.get()).isEqualTo(1);
            assertThat(result.conflictCount.get()).isEqualTo(threadCount - 1);

            // DB 검증 — 타깃 좌석이 RESERVED 상태이고 reservedBy 가 채워져 있음
            Seat persisted = seatRepository.findById(target).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(SeatStatus.RESERVED);
            assertThat(persisted.getReservedBy()).isNotNull();
        }

        @Test
        @DisplayName("TC-03: 같은 좌석 1개를 20명이 동시 점유 → 정확히 1명만 성공 (좌석 중복 0건)")
        void 동일좌석_다수_동시() throws Exception {
            // given — 첫 번째 좌석을 타깃으로
            Long target = seatIds.get(0);
            int threadCount = 20;

            // when
            ConcurrentResult result = runConcurrently(threadCount,
                    (userId, idx) -> seatReservationService.reserve(matchId, userId, List.of(target)));

            // then — 1명만 성공, 나머지 19명은 SEAT_ALREADY_RESERVED
            // 19명이 실패하는 경로는 2가지 (타이밍에 따라 혼재):
            //   1) 재시도 시점에 다시 fetch 했더니 이미 RESERVED → 즉시 거절
            //   2) @Version 충돌이 3회 연속 발생 → 재시도 모두 실패 후 종결
            // 어느 경로든 최종 결과는 SEAT_ALREADY_RESERVED 라 검증값은 동일
            assertThat(result.unexpected).isEmpty();
            assertThat(result.successCount.get()).isEqualTo(1);
            assertThat(result.conflictCount.get()).isEqualTo(threadCount - 1);

            // DB 검증 — 좌석 중복 예약 0건. 같은 좌석 행은 1개뿐이고 RESERVED.
            Seat persisted = seatRepository.findById(target).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(SeatStatus.RESERVED);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 다른 좌석 동시 점유
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("다른 좌석 동시 점유")
    class DifferentSeats {

        @Test
        @DisplayName("TC-02: 10명이 각자 다른 좌석 1개씩 동시 점유 → 모두 성공")
        void 다른좌석_10명_동시() throws Exception {
            // given — 10명이 각자 다른 좌석을 잡을 예정
            int threadCount = 10;

            // when — 각 스레드가 자기 인덱스에 해당하는 좌석을 점유
            ConcurrentResult result = runConcurrently(threadCount, (userId, idx) -> {
                Long mySeat = seatIds.get(idx);
                seatReservationService.reserve(matchId, userId, List.of(mySeat));
            });

            // then — 충돌 없이 전부 성공
            assertThat(result.unexpected).isEmpty();
            assertThat(result.conflictCount.get()).isZero();
            assertThat(result.successCount.get()).isEqualTo(threadCount);

            // DB 검증 — 0~9번 좌석이 모두 RESERVED
            for (int i = 0; i < threadCount; i++) {
                Seat s = seatRepository.findById(seatIds.get(i)).orElseThrow();
                assertThat(s.getStatus()).isEqualTo(SeatStatus.RESERVED);
            }
            // 10번 이후 좌석은 AVAILABLE 그대로
            Seat untouched = seatRepository.findById(seatIds.get(threadCount)).orElseThrow();
            assertThat(untouched.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 묶음 점유 부분 충돌
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("좌석 묶음 점유 — 부분 충돌")
    class PartialConflict {

        @Test
        @DisplayName("TC-04: A=[s1,s2], B=[s2,s3] 동시 점유 → 한 명은 성공, 한 명은 s2 충돌")
        void 묶음_부분_충돌() throws Exception {
            // given — maxTicketsPerUser=2 라 묶음 크기 2개씩
            Long s1 = seatIds.get(0);
            Long s2 = seatIds.get(1);
            Long s3 = seatIds.get(2);

            int threadCount = 2;
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch  = new CountDownLatch(threadCount);
            ConcurrentResult result = new ConcurrentResult();

            // when — A=[s1,s2], B=[s2,s3] 두 사용자가 동시에 묶음 점유 시도
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            executor.submit(() -> tryReserve(1001L, List.of(s1, s2),
                    readyLatch, startLatch, doneLatch, result));
            executor.submit(() -> tryReserve(1002L, List.of(s2, s3),
                    readyLatch, startLatch, doneLatch, result));

            readyLatch.await();         // 두 스레드 모두 준비 완료
            startLatch.countDown();     // 동시 시작
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // then — 한 명은 성공, 한 명은 SEAT_ALREADY_RESERVED
            assertThat(result.unexpected).isEmpty();
            assertThat(result.successCount.get()).isEqualTo(1);
            assertThat(result.conflictCount.get()).isEqualTo(1);

            // DB 검증 — s2 는 정확히 한 사람의 점유. 둘 중 한 명의 ID 가 reservedBy 에 들어가 있음
            Seat persistedS2 = seatRepository.findById(s2).orElseThrow();
            assertThat(persistedS2.getStatus()).isEqualTo(SeatStatus.RESERVED);
            assertThat(persistedS2.getReservedBy()).isIn(1001L, 1002L);
        }
    }

    // ──────── 동시성 헬퍼 ──────────────────────────────────────────────

    /**
     * 스레드 N개를 "출발선에 모아놨다가 동시에 출발"시키는 헬퍼
     * <p>
     * 그냥 submit() 하면 스레드가 띄엄띄엄 깨어나서 충돌이 거의 안 남
     * → 래치 3개로 출발 타이밍을 강제 동기화:
     * readyLatch  : 전원 준비 완료까지 대기
     * startLatch  : 동시에 출발 신호 발사 (충돌 빈도 ↑)
     * doneLatch   : 전원 종료까지 대기 (10초 안전장치)
     * <p>
     * 람다는 (userId, idx) 두 개를 받음. idx 가 필요 없는 테스트는 그냥 안 써도 됨
     */
    private ConcurrentResult runConcurrently(int threadCount, ReserveAction action)
            throws InterruptedException {
        ConcurrentResult result = new ConcurrentResult();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            final long userId = 1000L + i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();     // 준비 완료
                    startLatch.await();         // 출발 신호 대기
                    action.run(userId, idx);
                    result.successCount.incrementAndGet();
                } catch (BusinessException e) {
                    // 좌석 충돌/판매완료는 예상된 비즈니스 결과 → conflict 로 집계
                    if (e.getErrorCode() == ErrorCode.SEAT_ALREADY_RESERVED
                            || e.getErrorCode() == ErrorCode.SEAT_ALREADY_SOLD) {
                        result.conflictCount.incrementAndGet();
                    } else {
                        result.unexpected.add(e);
                    }
                } catch (Throwable t) {
                    result.unexpected.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();

        boolean allFinished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        if (!allFinished) {
            throw new IllegalStateException("동시성 테스트 타임아웃 — 데드락 의심");
        }

        return result;
    }

    /** 부분 충돌 시나리오 전용 — 묶음 점유를 한 번 실행 */
    private void tryReserve(long userId, List<Long> targetSeats,
                            CountDownLatch readyLatch, CountDownLatch startLatch,
                            CountDownLatch doneLatch,
                            ConcurrentResult result) {
        try {
            readyLatch.countDown();
            startLatch.await();
            seatReservationService.reserve(matchId, userId, targetSeats);
            result.successCount.incrementAndGet();
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.SEAT_ALREADY_RESERVED
                    || e.getErrorCode() == ErrorCode.SEAT_ALREADY_SOLD) {
                result.conflictCount.incrementAndGet();
            } else {
                result.unexpected.add(e);
            }
        } catch (Throwable t) {
            result.unexpected.add(t);
        } finally {
            doneLatch.countDown();
        }
    }

    // runConcurrently() 에 람다로 "할 일"을 넘기기 위한 인터페이스
    // (메서드 1개라 람다((userId, idx) -> { ... }) 로 바로 구현 가능)
    private interface ReserveAction {
        void run(long userId, int idx);
    }

    // 동시 실행 결과 모음 (성공 개수, 충돌 개수, 예상 못한 에러 목록)
    private static class ConcurrentResult {
        final AtomicInteger successCount  = new AtomicInteger();
        final AtomicInteger conflictCount = new AtomicInteger();
        final List<Throwable> unexpected  = new CopyOnWriteArrayList<>();
    }

    // ──────── 시드 빌더 ──────────────────────────────────────────────

    /** 예매 가능한 상태의 Event 생성. title 은 nanoTime 으로 충돌 방지 */
    private Event buildEvent() {
        LocalDateTime now = LocalDateTime.now();
        return Event.builder()
                .title("동시성 테스트 이벤트 " + System.nanoTime())
                .sportType(SportType.LOL)
                .place("테스트 경기장")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .bookingOpenAt(now.minusDays(1))
                .bookingCloseAt(now.plusDays(7))
                .maxTicketsPerUser(MAX_TICKETS_PER_USER)
                .cancelFee(0)
                .status(EventStatus.OPEN)
                .build();
    }

    /** 시작 시각이 미래인 Match (status 미지정 시 SCHEDULED) */
    private Match buildMatch(Event event) {
        return Match.builder()
                .event(event)
                .roundLabel("1R")
                .matchDate(LocalDate.now().plusDays(1))
                .startAt(LocalDateTime.now().plusDays(1))
                .build();
    }
}
