package com.ticketmaster.backend.domain.payment.integration;


import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.entity.EventStatus;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.payment.entity.Payment;
import com.ticketmaster.backend.domain.payment.entity.PaymentStatus;
import com.ticketmaster.backend.domain.payment.repository.PaymentRepository;
import com.ticketmaster.backend.domain.payment.service.PaymentFailureRecorder;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * 토스 승인 실패 시 recordFailure 가 REQUIRES_NEW 로 분리된 트랜잭션에 Payment FAILED 를 저장
 * 호출자가 예외를 던져 부모가 rollback 돼도 FAILED 는 살아남는지 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@DisplayName("PaymentFailureRecorder REQUIRES_NEW 통합 테스트")
public class PaymentFailureRecorderIT {

    @Container
    @ServiceConnection
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
            .withReuse(true);

    @Autowired
    private PaymentFailureRecorder paymentFailureRecorder;

    @Autowired
    private PaymentRepository paymentRepository;

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
        bookingId = transactionTemplate.execute(s -> {
            User user = userRepository.save(buildUser());
            Event event = eventRepository.save(buildEvent());
            Match match = matchRepository.save(buildMatch(event));
            Booking booking = Booking.create(user, match,
                    "b-fail-" + System.nanoTime(), 50000);
            return bookingRepository.save(booking).getId();
        });
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("부모 트랜잭션이 RuntimeException 으로 롤백돼도 Payment FAILED 는 보존")
    void requires_new_보존() {
        // given — setUp 에서 PENDING Booking 시드 완료

        // when — 부모 트랜잭션 안에서 booking 다시 fetch → recordFailure 호출 후 강제 RuntimeException
        assertThatThrownBy(() ->
                transactionTemplate.executeWithoutResult(s -> {
                    Booking booking = bookingRepository.findById(bookingId).orElseThrow();
                    paymentFailureRecorder.recordFailure(
                            booking, "pk-fail", "order-fail", 50000, "토스 승인 실패");
                    throw new RuntimeException("부모 강제 롤백");
                }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("부모 강제 롤백");

        // then — 부모 롤백과 무관하게 Payment FAILED 가 저장돼 있어야 함
        assertThat(paymentRepository.findByPaymentKey("pk-fail"))
                .isPresent()
                .get()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.FAILED);
    }

    // ------ 테스트 픽스처 ----------------------------------------

    private User buildUser() {
        return User.create(
                "fail-" + System.nanoTime() + "@test.com",
                "test-encoded-password",
                "fail-user",
                null
        );
    }
    private Event buildEvent() {
        return Event.builder()
                .title("실패-" + System.nanoTime())
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
