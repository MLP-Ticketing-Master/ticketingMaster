package com.ticketmaster.backend.domain.seat.service;

import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReleaseResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReserveResponse;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SeatReservationServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatReservationService seatReservationService;

    private Event event;
    private Match match;
    private Section section;
    private SeatGrade vip;

    private static final Long MATCH_ID = 10L;
    private static final Long USER_ID = 99L;
    private static final Long OTHER_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        event = createEvent(1L, 2);  // maxTicketsPerUser = 2
        match = createMatch(MATCH_ID, event);
        section = createSection(101L, event, "좌측", 1);
        vip = createSeatGrade(201L, event, "VIP", 100000, "#FFD700");

        // self 필드는 @InjectMocks 가 못 채움 — 자기 자신으로 직접 주입
        // 재시도 시나리오를 다루는 nested 클래스에선 spy 로 재설정
        ReflectionTestUtils.setField(seatReservationService, "self", seatReservationService);
    }

    // ----- 점유 (reserve) ------------------------------------

    @Nested
    @DisplayName("좌석 점유 — reserve")
    class Reserve {

        @Test
        @DisplayName("TC-01: 정상 점유 — reservedSeatIds / reservedUntil / totalPrice 반환")
        void 정상_점유() {
            // given — 매치, AVAILABLE 좌석 2개
            given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));

            Seat s1 = createSeat(1L, match, section, vip, "A", 1, "VIP-A-1", SeatStatus.AVAILABLE, null);
            Seat s2 = createSeat(2L, match, section, vip, "A", 2, "VIP-A-2", SeatStatus.AVAILABLE, null);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L, 2L)))
                    .willReturn(List.of(s1, s2));

            // when
            SeatReserveResponse response = seatReservationService.reserve(MATCH_ID, USER_ID, List.of(1L, 2L));

            // then
            assertThat(response.getReservedSeatIds()).containsExactly(1L, 2L);
            assertThat(response.getTotalPrice()).isEqualTo(200000); // VIP 100,000 x 2
            assertThat(response.getReservedUntil()).isAfter(LocalDateTime.now());

            // 좌석 상태가 RESERVED 로 바뀌고 reservedBy 가 채워졌는지
            assertThat(s1.getStatus()).isEqualTo(SeatStatus.RESERVED);
            assertThat(s1.getReservedBy()).isEqualTo(USER_ID);
            assertThat(s2.getStatus()).isEqualTo(SeatStatus.RESERVED);
            assertThat(s2.getReservedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("TC-02: maxTicketsPerUser 초과 → MAX_TICKETS_EXCEEDED")
        void 한도_초과() {
            // given — maxTicketsPerUser = 2 인데 3매 요청
            given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));

            // when & then
            assertThatThrownBy(() ->
                    seatReservationService.reserve(MATCH_ID, USER_ID, List.of(1L, 2L, 3L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_TICKETS_EXCEEDED);
        }

        @Test
        @DisplayName("TC-03: 존재하지 않는 매치 → MATCH_NOT_FOUND")
        void 매치_없음() {
            // given
            given(matchRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    seatReservationService.reserve(999L, USER_ID, List.of(1L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
        }

        @Test
        @DisplayName("TC-04: 좌석 일부 누락 → SEAT_NOT_FOUND")
        void 좌석_일부_누락() {
            // given — 2개 요청했는데 1개만 조회됨
            given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));
            Seat s1 = createSeat(1L, match, section, vip, "A", 1, "VIP-A-1", SeatStatus.AVAILABLE, null);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L, 999L)))
                    .willReturn(List.of(s1));

            // when & then
            assertThatThrownBy(() ->
                    seatReservationService.reserve(MATCH_ID, USER_ID, List.of(1L, 999L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
        }

        @Test
        @DisplayName("TC-05: SOLD 포함 → SEAT_ALREADY_SOLD + conflictSeatIds")
        void SOLD_포함() {
            // given
            given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));
            Seat s1 = createSeat(1L, match, section, vip, "A", 1, "VIP-A-1", SeatStatus.AVAILABLE, null);
            Seat s2 = createSeat(2L, match, section, vip, "A", 2, "VIP-A-2", SeatStatus.SOLD, null);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L, 2L)))
                    .willReturn(List.of(s1, s2));

            // when & then
            assertThatThrownBy(() ->
                    seatReservationService.reserve(MATCH_ID, USER_ID, List.of(1L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_SOLD)
                    .hasFieldOrPropertyWithValue("conflictSeatIds", List.of(2L));
        }

        @Test
        @DisplayName("TC-06: RESERVED 포함 → SEAT_ALREADY_RESERVED + conflictSeatIds")
        void RESERVED_포함() {
            // given
            given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));
            Seat s1 = createSeat(1L, match, section, vip, "A", 1, "VIP-A-1", SeatStatus.AVAILABLE, null);
            Seat s2 = createSeat(2L, match, section, vip, "A", 2, "VIP-A-2", SeatStatus.RESERVED, OTHER_USER_ID);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L, 2L)))
                    .willReturn(List.of(s1, s2));

            // when & then
            assertThatThrownBy(() ->
                    seatReservationService.reserve(MATCH_ID, USER_ID, List.of(1L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_RESERVED)
                    .hasFieldOrPropertyWithValue("conflictSeatIds", List.of(2L));
        }
    }

    // ----- 점유 재시도 (reserve + 낙관적 락 충돌) -------------

    @Nested
    @DisplayName("좌석 점유 — 재시도")
    class Retry {

        @Test
        @DisplayName("TC-07: 1회 충돌 후 재시도 성공 → tryReserveOnce 2회 호출")
        void 재시도_성공() {
            // tryReserveOnce 호출 결과를 제어하기 위해 spy 사용
            // given — service 자체를 spy 로 감싸고 self 도 spy 로 재주입
            SeatReservationService spy = Mockito.spy(seatReservationService);
            ReflectionTestUtils.setField(spy, "self", spy);

            SeatReserveResponse ok = SeatReserveResponse.of(
                    List.of(1L), LocalDateTime.now().plusMinutes(7), 100000);

            // 첫 호출은 OptimisticLockingFailureException, 두 번째는 정상 반환
            Mockito.doThrow(new OptimisticLockingFailureException("conflict"))
                    .doReturn(ok)
                    .when(spy).tryReserveOnce(MATCH_ID, USER_ID, List.of(1L));

            // when
            SeatReserveResponse response = spy.reserve(MATCH_ID, USER_ID, List.of(1L));

            // then
            assertThat(response).isEqualTo(ok);
            Mockito.verify(spy, Mockito.times(2))
                    .tryReserveOnce(MATCH_ID, USER_ID, List.of(1L));
        }

        @Test
        @DisplayName("TC-08: 3회 모두 충돌 → SEAT_ALREADY_RESERVED + 요청 seatIds")
        void 재시도_모두_실패() {
            // given
            SeatReservationService spy = Mockito.spy(seatReservationService);
            ReflectionTestUtils.setField(spy, "self", spy);

            Mockito.doThrow(new OptimisticLockingFailureException("conflict"))
                    .when(spy).tryReserveOnce(MATCH_ID, USER_ID, List.of(1L, 2L));

            // when & then — 마지막 재시도 실패 시 요청 seatIds 를 conflictSeatIds 로 반환
            assertThatThrownBy(() -> spy.reserve(MATCH_ID, USER_ID, List.of(1L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_ALREADY_RESERVED)
                    .hasFieldOrPropertyWithValue("conflictSeatIds", List.of(1L, 2L));

            // 최초 1회 + 재시도 2회 = 총 3번 호출
            Mockito.verify(spy, Mockito.times(3))
                    .tryReserveOnce(MATCH_ID, USER_ID, List.of(1L, 2L));
        }
    }

    // ----- 해제 (release) ------------------------------------

    @Nested
    @DisplayName("좌석 해제 — release")
    class Release {

        @Test
        @DisplayName("TC-09: 본인 점유 좌석만 해제 — 다른 사용자 점유 / AVAILABLE 좌석은 응답에서 제외")
        void 본인_점유만_해제() {
            // given — 1번은 본인 점유, 2번은 다른 사용자 점유, 3번은 이미 AVAILABLE
            Seat s1 = createSeat(1L, match, section, vip, "A", 1, "VIP-A-1", SeatStatus.RESERVED, USER_ID);
            Seat s2 = createSeat(2L, match, section, vip, "A", 2, "VIP-A-2", SeatStatus.RESERVED, OTHER_USER_ID);
            Seat s3 = createSeat(3L, match, section, vip, "A", 3, "VIP-A-3", SeatStatus.AVAILABLE, null);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L, 2L, 3L)))
                    .willReturn(List.of(s1, s2, s3));

            // when
            SeatReleaseResponse response = seatReservationService.release(MATCH_ID, USER_ID, List.of(1L, 2L, 3L));

            // then — 1번만 해제됨
            assertThat(response.getReleasedSeatIds()).containsExactly(1L);

            assertThat(s1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(s1.getReservedBy()).isNull();

            // 다른 사용자 좌석은 그대로
            assertThat(s2.getStatus()).isEqualTo(SeatStatus.RESERVED);
            assertThat(s2.getReservedBy()).isEqualTo(OTHER_USER_ID);
        }

        @Test
        @DisplayName("TC-10: 해제할 좌석이 없어도 200 OK + 빈 리스트 (멱등성)")
        void 멱등성_빈_리스트() {
            // given — 요청한 좌석 모두 본인 점유 아님
            Seat s1 = createSeat(1L, match, section, vip, "A", 1, "VIP-A-1", SeatStatus.AVAILABLE, null);
            given(seatRepository.findByMatchAndIdIn(MATCH_ID, List.of(1L)))
                    .willReturn(List.of(s1));

            // when
            SeatReleaseResponse response = seatReservationService.release(MATCH_ID, USER_ID, List.of(1L));

            // then
            assertThat(response.getReleasedSeatIds()).isEmpty();
        }
    }

    // ----- 헬퍼 ------------------------------------

    private Event createEvent(Long id, int maxTickets) {
        Event e = Event.builder().maxTicketsPerUser(maxTickets).build();
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private Match createMatch(Long id, Event event) {
        Match m = Match.builder()
                .event(event)
                .roundLabel("1R")
                .matchDate(LocalDate.now())
                .startAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private Section createSection(Long id, Event event, String name, int displayOrder) {
        Section s = Section.create(event, name, displayOrder, null);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    private SeatGrade createSeatGrade(Long id, Event event, String code, int price, String colorHex) {
        SeatGrade g = SeatGrade.create(event, code, price, colorHex);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    private Seat createSeat(Long id, Match match, Section section, SeatGrade grade,
                            String rowLabel, int seatNo, String seatCode,
                            SeatStatus status, Long reservedBy) {
        Seat s = Seat.create(match, section, grade, rowLabel, seatNo, seatCode);
        ReflectionTestUtils.setField(s, "id", id);
        ReflectionTestUtils.setField(s, "status", status);
        if (reservedBy != null) ReflectionTestUtils.setField(s, "reservedBy", reservedBy);
        return s;
    }
}