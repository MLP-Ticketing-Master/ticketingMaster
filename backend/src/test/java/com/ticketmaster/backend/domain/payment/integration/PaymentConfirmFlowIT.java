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
import com.ticketmaster.backend.domain.payment.dto.response.PaymentResponse;
import com.ticketmaster.backend.domain.payment.entity.Payment;
import com.ticketmaster.backend.domain.payment.entity.PaymentStatus;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import com.ticketmaster.backend.domain.payment.service.PaymentService;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentResponse;
import com.ticketmaster.backend.domain.payment.toss.TossPaymentsClient;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * PaymentService.confirm() 로직이 한 트랜잭션 안에서
 * Booking PENDING→CONFIRMED + Seat RESERVED→SOLD + Payment SUCCESS
 * 세 도메인을 일괄 commit 하는지 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@DisplayName("결제 confirm 정상 흐름 통합 테스트")
public class PaymentConfirmFlowIT {

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

    @MockitoBean
    private TossPaymentsClient tossClient;

    private Long bookingId;
    private Long userId;
    private List<Long> seatIds;
    private int totalPrice;

    @BeforeEach
    void setUp() {
        // PENDING Booking + RESERVED 좌석 2개 시드 — PaymentService.confirm 사전 검증 통과
        transactionTemplate.executeWithoutResult(s -> {
            User user = userRepository.save(buildUser());
            Event event = eventRepository.save(buildEvent());
            Match match = matchRepository.save(buildMatch(event));
            Section section = sectionRepository.save(Section.create(event, "좌측", 1, null));
            SeatGrade vip = seatGradeRepository.save(SeatGrade.create(event, "VIP", 50000, "#FFD700"));

            // 좌석 2개 RESERVED — reservedBy=userId, reservedUntil=+7
            // 여러 좌석이 모두 SOLD 될지 검증하려고 1개 아닌 2개로 시드
            List<Long> seedSeatIds = new ArrayList<>();
            Booking booking = Booking.create(user, match,
                    "b-flow-" + System.nanoTime(), 100000);

            for (int i = 1; i <= 2; i++) {
                Seat seat = Seat.create(match, section, vip, "A", i, "VIP-A-" + i);
                seat.reserve(user.getId(), LocalDateTime.now().plusMinutes(7));
                seatRepository.save(seat);
                booking.addBookingSeat(BookingSeat.of(seat, 50000));
                seedSeatIds.add(seat.getId());
            }
            bookingRepository.save(booking);

            userId = user.getId();
            bookingId = booking.getId();
            seatIds = seedSeatIds;
            totalPrice = 100000;
        });
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
    @DisplayName("토스 승인 성공 → Booking CONFIRMED + Seat SOLD + Payment SUCCESS 일괄 commit")
    void happy_path() {
        // given — 토스 mock SUCCESS
        given(tossClient.confirm("pk-flow", "order-flow", totalPrice))
                .willReturn(buildTossSuccess(totalPrice));
        PaymentConfirmRequest req = buildConfirmRequest("pk-flow", "order-flow", bookingId, totalPrice);

        // when
        PaymentResponse res = paymentService.confirm(req, userId);

        // then — 세 도메인이 일괄 commit 되어 모두 변경되었는지
        assertThat(res).isNotNull();

        // 1. Booking PENDING → CONFIRMED
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // 2. 좌석 전부 RESERVED → SOLD
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId).orElseThrow();
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD);
        }

        // 3. Payment SUCCESS 저장 (paymentKey / amount 일치)
        Payment payment = paymentRepository.findByPaymentKey("pk-flow").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getAmount()).isEqualTo(totalPrice);
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

    /** 토스 API SUCCESS 응답 시뮬레이션 */
    private TossPaymentResponse buildTossSuccess(int amount) {
        TossPaymentResponse res = new TossPaymentResponse();
        ReflectionTestUtils.setField(res, "paymentKey", "pk-flow");
        ReflectionTestUtils.setField(res, "orderId", "order-flow");
        ReflectionTestUtils.setField(res, "status", "DONE");
        ReflectionTestUtils.setField(res, "method", "카드");
        ReflectionTestUtils.setField(res, "totalAmount", amount);
        ReflectionTestUtils.setField(res, "approvedAt", OffsetDateTime.now());
        return res;
    }

    // ──────── 테스트 픽스처 ────────

    private User buildUser() {
        return User.create(
                "flow-" + System.nanoTime() + "@test.com",
                "dummy-encoded-password",
                "flow-user",
                null
        );
    }

    private Event buildEvent() {
        return Event.builder()
                .title("플로우-" + System.nanoTime())
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
