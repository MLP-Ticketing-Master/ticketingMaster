package com.ticketmaster.backend.domain.payment.integration;

import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingSeat;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.payment.dto.request.PaymentConfirmRequest;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import com.ticketmaster.backend.domain.payment.service.PaymentService;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentResponse;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentsClient;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Payment.paymentKey 컬럼은 UNIQUE
 * 같은 paymentKey 로 동시에 confirm 2회 호출 시 한 쪽만 저장되는지 검증
 * 멱등성 분기 hit / UNIQUE 제약 차단 두 경로 모두 Payment count = 1 보장
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@DisplayName("결제 멱등성 통합 테스트 — paymentKey 동시 호출")
public class PaymentIdempotencyIT {

    @Container
    @ServiceConnection
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
            .withReuse(true);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatGradeRepository seatGradeRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 토스 호출은 mock — 동시 호출 시 항상 같은 SUCCESS 응답 반환
     */
    @MockitoBean
    private TossPaymentsClient tossClient;

    private Long bookingId;
    private Long userId;

    @BeforeEach
    void setUp() {
        // PENDING Booking + RESERVED 좌석 시드 — PaymentService.confirm 사전 검증 통과시키기
        transactionTemplate.executeWithoutResult(s -> {
            User user = userRepository.save(buildUser());
            Event event = eventRepository.save(buildEvent());
            Match match = matchRepository.save(buildMatch(event));
            Section section = sectionRepository.save(Section.create(event, "좌측", 1, null));
            SeatGrade vip = seatGradeRepository.save(SeatGrade.create(event, "VIP", 50000, "#FFD700"));

            // 좌석 1개 RESERVED — reservedBy=userId, reservedUntil=+7
            Seat seat = Seat.create(match, section, vip, "A", 1, "VIP-A-1");
            seat.reserve(user.getId(), LocalDateTime.now().plusMinutes(7));
            seatRepository.save(seat);

            // PENDING Booking + BookingSeat 연결
            Booking booking = Booking.create(user, match,
                    "b-test-" + System.nanoTime(), 50000);
            booking.addBookingSeat(BookingSeat.of(seat, 50000));
            bookingRepository.save(booking);

            userId = user.getId();
            bookingId = booking.getId();
        });

        // 토스 mock — 동시 호출이라도 항상 같은 SUCCESS 응답
        given(tossClient.confirm(anyString(), anyString(), anyInt()))
                .willReturn(buildTossSuccess(50000));
    }

    @AfterEach
    void tearDown() {
        // FK 의존 역순 정리
        paymentRepository.deleteAllInBatch();
        // deleteAllInBatch 는 JPQL bulk delete 라 cascade 무시 → deleteAll 로 BookingSeat 자동 삭제
        bookingRepository.deleteAll();
        seatRepository.deleteAllInBatch();
        seatGradeRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("같은 paymentKey 로 동시 2회 confirm → Payment 1건만, Booking CONFIRMED 1회만")
    void 동시_2회_호출_1건만() throws Exception {
        // given — 동일 paymentKey 재사용
        PaymentConfirmRequest req = buildConfirmRequest("pk-same", "order-same", bookingId, 50000);
        int threadCount = 2;

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        // when — 두 스레드 동시 confirm
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    paymentService.confirm(req, userId);
                    // 정상 종료 — 멱등성 분기 hit 또는 첫 호출 성공
                    successCount.incrementAndGet();
                } catch (DataIntegrityViolationException
                         | ObjectOptimisticLockingFailureException e) {
                    // UNIQUE 충돌 or @Version 충돌
                    conflictCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // then — 어느 경로든 Payment 는 정확히 1건
        // 시나리오 1: 두 요청이 순차적으로 들어와 둘 다 예외 없이 성공
        //   - 첫 번째: 실제 저장
        //   - 두 번째: 멱등 분기에 걸려 기존 payment 반환
        //   - → successCount = 2, conflictCount = 0

        // 시나리오 2: 두 요청이 동시에 멱등 분기를 통과 (race condition)
        //   - 한 쪽: 저장 성공
        //   - 다른 쪽: UNIQUE 또는 OptimisticLock 예외로 차단
        //   - → successCount = 1, conflictCount = 1
        assertThat(successCount.get() + conflictCount.get()).isEqualTo(threadCount);
        assertThat(paymentRepository.findByPaymentKey("pk-same")).isPresent();
        assertThat(paymentRepository.count()).isEqualTo(1);

        // Booking CONFIRMED 한 번만
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    // ──────── 헬퍼 ────────

    private PaymentConfirmRequest buildConfirmRequest(String paymentKey, String orderId,
                                                      Long bookingId, int amount) {
        PaymentConfirmRequest req = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(req, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(req, "orderId", orderId);
        ReflectionTestUtils.setField(req, "bookingId", bookingId);
        ReflectionTestUtils.setField(req, "amount", amount);
        return req;
    }

    /**
     * 토스 API SUCCESS 응답 시뮬레이션
     */
    private TossPaymentResponse buildTossSuccess(int amount) {
        TossPaymentResponse res = new TossPaymentResponse();
        ReflectionTestUtils.setField(res, "paymentKey", "pk-same");
        ReflectionTestUtils.setField(res, "orderId", "order-same");
        ReflectionTestUtils.setField(res, "status", "DONE");
        ReflectionTestUtils.setField(res, "method", "카드");
        ReflectionTestUtils.setField(res, "totalAmount", amount);
        ReflectionTestUtils.setField(res, "approvedAt", LocalDateTime.now());
        return res;
    }

    // ──────── 테스트 픽스처 ────────

    private User buildUser() {
        return User.create(
                "test-" + System.nanoTime() + "@test.com",
                "test-encoded-password",
                "test-user",
                null
        );
    }

    private Event buildEvent() {
        return Event.builder()
                .title("멱등성-" + System.nanoTime())
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
