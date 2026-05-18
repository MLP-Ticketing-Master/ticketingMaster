package com.ticketmaster.backend.domain.booking.service;

import com.ticketmaster.backend.domain.booking.dto.request.BookingCreateRequest;
import com.ticketmaster.backend.domain.booking.dto.response.BookingResponse;
import com.ticketmaster.backend.domain.booking.dto.response.BookingSummaryResponse;
import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.domain.user.repository.UserRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
// setUp()의 공통 stubbing이 검증 실패/조회 테스트에서 안 쓰여도 예외가 안 터지도록 LENIENT 설정
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    @Mock MatchRepository matchRepository;
    @Mock SeatRepository seatRepository;
    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;

    @InjectMocks BookingService bookingService;

    private static final Long USER_ID  = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long MATCH_ID = 10L;

    private Event event;
    private Match match;
    private User user;
    private SeatGrade vip;
    private Section section;

    @BeforeEach
    void setUp() {
        event   = createEvent(1L, 2);
        match   = createMatch(MATCH_ID, event);
        user    = createUser(USER_ID);
        section = createSection(100L, event, "A구역", 1);
        vip     = createSeatGrade(200L, event, "VIP", 100_000, "#FFD700");

        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    }

    // -------------------------------------------------------
    // 예매 생성 — 정상 흐름
    // -------------------------------------------------------

    @Nested
    @DisplayName("예매 생성 — 정상 흐름")
    class CreateBookingSuccess {

        @Test
        @DisplayName("TC-01: 본인 점유 좌석으로 예매 생성 → 201, Booking + BookingSeat 생성")
        void 정상_예매_생성() {
            // given
            LocalDateTime future = LocalDateTime.now().plusMinutes(5);
            Seat s1 = reservedSeat(1L, USER_ID, future, 100_000);
            Seat s2 = reservedSeat(2L, USER_ID, future, 100_000);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L, 2L))).willReturn(List.of(s1, s2));

            // save() 호출 시 반환 객체에 id 를 직접 세팅 → savedBooking.getId() == 99L
            given(bookingRepository.save(any())).willAnswer(inv -> {
                Booking b = inv.getArgument(0);
                ReflectionTestUtils.setField(b, "id", 99L);
                return b;
            });

            Booking detail = captureAndIdBooking(99L, user, match, 200_000);
            given(bookingRepository.findDetailByIdForUser(99L)).willReturn(Optional.of(detail));

            BookingCreateRequest req = createRequest(MATCH_ID, List.of(1L, 2L));

            // when
            BookingResponse response = bookingService.createBooking(USER_ID, req);

            // then
            assertThat(response.getBookingId()).isEqualTo(99L);
            assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("TC-02: totalPrice = SeatGrade.price 합 일치 확인")
        void totalPrice_정확성() {
            SeatGrade r = createSeatGrade(201L, event, "R", 80_000, "#C0C0C0");
            LocalDateTime future = LocalDateTime.now().plusMinutes(5);
            Seat s1 = reservedSeat(1L, USER_ID, future, 100_000);   // VIP
            Seat s2 = reservedSeatWithGrade(2L, USER_ID, future, r); // R

            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L, 2L))).willReturn(List.of(s1, s2));

            given(bookingRepository.save(any())).willAnswer(inv -> {
                Booking b = inv.getArgument(0);
                assertThat(b.getTotalPrice()).isEqualTo(180_000);
                ReflectionTestUtils.setField(b, "id", 1L);
                given(bookingRepository.findDetailByIdForUser(1L)).willReturn(Optional.of(b));
                return b;
            });

            bookingService.createBooking(USER_ID, createRequest(MATCH_ID, List.of(1L, 2L)));
        }

        @Test
        @DisplayName("TC-03: BookingSeat.seatPrice 스냅샷 저장 확인 (등급가 변경 후에도 불변)")
        void seatPrice_스냅샷_저장() {
            LocalDateTime future = LocalDateTime.now().plusMinutes(5);
            Seat s1 = reservedSeat(1L, USER_ID, future, 100_000);

            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L))).willReturn(List.of(s1));
            given(bookingRepository.save(any())).willAnswer(inv -> {
                Booking b = inv.getArgument(0);
                assertThat(b.getBookingSeats()).hasSize(1);
                assertThat(b.getBookingSeats().get(0).getSeatPrice()).isEqualTo(100_000);
                ReflectionTestUtils.setField(b, "id", 1L);
                given(bookingRepository.findDetailByIdForUser(1L)).willReturn(Optional.of(b));
                return b;
            });

            bookingService.createBooking(USER_ID, createRequest(MATCH_ID, List.of(1L)));
        }
    }

    // -------------------------------------------------------
    // 예매 생성 — 좌석 점유 검증 실패
    // -------------------------------------------------------

    @Nested
    @DisplayName("예매 생성 — 좌석 점유 검증")
    class CreateBookingValidation {

        @Test
        @DisplayName("TC-04: 다른 사용자 점유 좌석으로 요청 → 409 SEAT_ALREADY_RESERVED")
        void 타인_점유_좌석() {
            LocalDateTime future = LocalDateTime.now().plusMinutes(5);
            Seat s1 = reservedSeat(1L, OTHER_ID, future, 100_000);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L))).willReturn(List.of(s1));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, createRequest(MATCH_ID, List.of(1L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SEAT_ALREADY_RESERVED));
        }

        @Test
        @DisplayName("TC-05: 만료된 점유 (reservedUntil 경과) → 410 SEAT_RESERVATION_EXPIRED")
        void 점유_만료() {
            LocalDateTime past = LocalDateTime.now().minusSeconds(1);
            Seat s1 = reservedSeat(1L, USER_ID, past, 100_000);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L))).willReturn(List.of(s1));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, createRequest(MATCH_ID, List.of(1L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SEAT_RESERVATION_EXPIRED));
        }

        @Test
        @DisplayName("TC-06: AVAILABLE 상태 좌석으로 요청 → 409 SEAT_ALREADY_RESERVED")
        void AVAILABLE_좌석_예매_시도() {
            Seat s1 = availableSeat(1L, 100_000);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L))).willReturn(List.of(s1));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, createRequest(MATCH_ID, List.of(1L))))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SEAT_ALREADY_RESERVED));
        }
    }

    // -------------------------------------------------------
    // 예매 생성 — BookingSeat UNIQUE 동시성
    // -------------------------------------------------------

    @Nested
    @DisplayName("예매 생성 — BookingSeat UNIQUE 동시성")
    class BookingSeatUnique {

        @Test
        @DisplayName("TC-07: 동시 예매 시도 — DataIntegrityViolationException → 409 SEAT_ALREADY_RESERVED")
        void 동시_예매_UNIQUE_위반() {
            LocalDateTime future = LocalDateTime.now().plusMinutes(5);
            Seat s1 = reservedSeat(1L, USER_ID, future, 100_000);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L))).willReturn(List.of(s1));
            given(bookingRepository.save(any())).willThrow(new DataIntegrityViolationException("uk_booking_seat_match_seat"));

            assertThatThrownBy(() -> bookingService.createBooking(USER_ID, createRequest(MATCH_ID, List.of(1L))))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("TC-08: GlobalExceptionHandler가 DataIntegrityViolationException → SEAT_ALREADY_RESERVED(409) 매핑 검증")
        void 예외_핸들러_매핑() {
            assertThat(ErrorCode.SEAT_ALREADY_RESERVED.getHttpStatus().value()).isEqualTo(409);
        }
    }

    // -------------------------------------------------------
    // 단건 조회
    // -------------------------------------------------------

    @Nested
    @DisplayName("단건 조회 — getBooking")
    class GetBooking {

        @Test
        @DisplayName("TC-09: 본인 예매 조회 → 200")
        void 본인_예매_조회() {
            Booking booking = captureAndIdBooking(1L, user, match, 100_000);
            given(bookingRepository.findDetailByIdForUser(1L)).willReturn(Optional.of(booking));

            BookingResponse response = bookingService.getBooking(USER_ID, false, 1L);

            assertThat(response.getBookingId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC-10: 타인 예매 조회 → 403 BOOKING_ACCESS_DENIED")
        void 타인_예매_조회() {
            User otherUser = createUser(OTHER_ID);
            Booking booking = captureAndIdBooking(1L, otherUser, match, 100_000);
            given(bookingRepository.findDetailByIdForUser(1L)).willReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.getBooking(USER_ID, false, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BOOKING_ACCESS_DENIED));
        }

        @Test
        @DisplayName("TC-11: 존재하지 않는 bookingId → 404 BOOKING_NOT_FOUND")
        void 존재하지_않는_예매() {
            given(bookingRepository.findDetailByIdForUser(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBooking(USER_ID, false, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BOOKING_NOT_FOUND));
        }
    }

    // -------------------------------------------------------
    // 내 예매 목록
    // -------------------------------------------------------

    @Nested
    @DisplayName("내 예매 목록 — getMyBookings")
    class GetMyBookings {

        @Test
        @DisplayName("TC-12: 본인 예매 전체 반환")
        void 전체_목록() {
            Booking b1 = captureAndIdBooking(1L, user, match, 100_000);
            Booking b2 = captureAndIdBooking(2L, user, match, 200_000);
            given(bookingRepository.findMyBookings(eq(USER_ID), isNull(), any()))
                    .willReturn(new PageImpl<>(List.of(b1, b2)));

            Page<BookingSummaryResponse> result = bookingService.getMyBookings(USER_ID, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("TC-13: status=CONFIRMED 필터 동작")
        void 상태_필터() {
            Booking b = captureAndIdBooking(1L, user, match, 100_000);
            ReflectionTestUtils.setField(b, "status", BookingStatus.CONFIRMED);
            given(bookingRepository.findMyBookings(eq(USER_ID), eq(BookingStatus.CONFIRMED), any()))
                    .willReturn(new PageImpl<>(List.of(b)));

            Page<BookingSummaryResponse> result = bookingService.getMyBookings(USER_ID, BookingStatus.CONFIRMED, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("TC-14: 최신순 정렬 확인 — id 내림차순 (Repository ORDER BY b.id DESC 보장)")
        void 최신순_정렬() {
            Booking older = captureAndIdBooking(1L, user, match, 100_000);
            Booking newer = captureAndIdBooking(2L, user, match, 100_000);
            given(bookingRepository.findMyBookings(eq(USER_ID), isNull(), any()))
                    .willReturn(new PageImpl<>(List.of(newer, older)));

            Page<BookingSummaryResponse> result = bookingService.getMyBookings(USER_ID, null, PageRequest.of(0, 10));

            List<BookingSummaryResponse> list = result.getContent();
            assertThat(list.get(0).getBookingId()).isEqualTo(2L);
            assertThat(list.get(1).getBookingId()).isEqualTo(1L);
        }
    }

    // -------------------------------------------------------
    // 헬퍼
    // -------------------------------------------------------

    private Event createEvent(Long id, int maxTickets) {
        Event e = Event.builder().maxTicketsPerUser(maxTickets).title("테스트 대회").build();
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private Match createMatch(Long id, Event event) {
        Match m = Match.builder()
                .event(event)
                .roundLabel("1R")
                .matchDate(LocalDate.now())
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                .bookingOpenAt(LocalDateTime.now().minusHours(1))
                .bookingCloseAt(LocalDateTime.now().plusHours(23))
                .build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private User createUser(Long id) {
        User u = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Section createSection(Long id, Event event, String name, int order) {
        Section s = Section.create(event, name, order, null);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    private SeatGrade createSeatGrade(Long id, Event event, String code, int price, String hex) {
        SeatGrade g = SeatGrade.create(event, code, price, hex);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    /** RESERVED 상태 + reservedBy/reservedUntil 세팅된 좌석 */
    private Seat reservedSeat(Long id, Long reservedBy, LocalDateTime reservedUntil, int price) {
        SeatGrade grade = createSeatGrade(200L + id, event, "VIP", price, "#FFD700");
        Seat s = Seat.create(match, section, grade, "A", id.intValue(), "VIP-A-" + id);
        ReflectionTestUtils.setField(s, "id", id);
        ReflectionTestUtils.setField(s, "status", SeatStatus.RESERVED);
        ReflectionTestUtils.setField(s, "reservedBy", reservedBy);
        ReflectionTestUtils.setField(s, "reservedUntil", reservedUntil);
        return s;
    }

    private Seat reservedSeatWithGrade(Long id, Long reservedBy, LocalDateTime reservedUntil, SeatGrade grade) {
        Seat s = Seat.create(match, section, grade, "B", id.intValue(), "R-B-" + id);
        ReflectionTestUtils.setField(s, "id", id);
        ReflectionTestUtils.setField(s, "status", SeatStatus.RESERVED);
        ReflectionTestUtils.setField(s, "reservedBy", reservedBy);
        ReflectionTestUtils.setField(s, "reservedUntil", reservedUntil);
        return s;
    }

    /** AVAILABLE 상태 좌석 (기본값) */
    private Seat availableSeat(Long id, int price) {
        SeatGrade grade = createSeatGrade(300L + id, event, "VIP", price, "#FFD700");
        Seat s = Seat.create(match, section, grade, "A", id.intValue(), "VIP-A-" + id);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    /** id 세팅된 Booking (findDetail / 목록 응답용 픽스처) */
    private Booking captureAndIdBooking(Long id, User user, Match match, int totalPrice) {
        Booking b = Booking.create(user, match, "BK20260518TEST01", totalPrice);
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    private BookingCreateRequest createRequest(Long matchId, List<Long> seatIds) {
        BookingCreateRequest req = new BookingCreateRequest();
        ReflectionTestUtils.setField(req, "matchId", matchId);
        ReflectionTestUtils.setField(req, "seatIds", seatIds);
        return req;
    }
}