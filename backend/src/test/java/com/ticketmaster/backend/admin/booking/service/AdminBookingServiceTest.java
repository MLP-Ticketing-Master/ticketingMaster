package com.ticketmaster.backend.admin.booking.service;

import com.ticketmaster.backend.admin.booking.dto.response.AdminBookingDetailResponse;
import com.ticketmaster.backend.admin.booking.dto.response.AdminBookingListResponse;
import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.booking.repository.BookingRepository;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBookingServiceTest {

    @InjectMocks
    private AdminBookingService service;

    @Mock
    private BookingRepository bookingRepository;

    private static final Long BOOKING_ID = 10L;

    // ─── 목록 조회 ──────────────────────────────────

    @Test
    @DisplayName("status_필터로_예매_목록_반환")
    void 예매_목록_조회_status필터() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Booking> page = new PageImpl<>(List.of(booking(BOOKING_ID)), pageable, 1);
        given(bookingRepository.findAllForAdmin(BookingStatus.CONFIRMED, pageable)).willReturn(page);

        // when
        Page<AdminBookingListResponse> result =
                service.getAllListBooking(BookingStatus.CONFIRMED, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        AdminBookingListResponse res = result.getContent().get(0);
        assertThat(res.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(res.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(res.getEventTitle()).isEqualTo("LOL 챔피언스 코리아 결승");
        verify(bookingRepository).findAllForAdmin(BookingStatus.CONFIRMED, pageable);
    }

    @Test
    @DisplayName("status_null이면_전체_조회")
    void 예매_목록_조회_status없음() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(bookingRepository.findAllForAdmin(null, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<AdminBookingListResponse> result = service.getAllListBooking(null, pageable);

        // then
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
        verify(bookingRepository).findAllForAdmin(null, pageable);
    }

    // ─── 상세 조회 ──────────────────────────────────

    @Test
    @DisplayName("예매_상세_조회_정상")
    void 예매_상세_조회_정상() {
        // given
        Booking b = booking(BOOKING_ID);
        given(bookingRepository.findDetailById(BOOKING_ID)).willReturn(Optional.of(b));

        // when
        AdminBookingDetailResponse result = service.getDetailBooking(BOOKING_ID);

        // then
        assertThat(result.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getEventTitle()).isEqualTo("LOL 챔피언스 코리아 결승");
        assertThat(result.getMatchInfo().getHomeTeamName()).isEqualTo("T1");
        assertThat(result.getMatchInfo().getAwayTeamName()).isEqualTo("KT");
    }

    @Test
    @DisplayName("예매_상세_조회_없으면_BOOKING_NOT_FOUND")
    void 예매_상세_조회_없음() {
        // given
        given(bookingRepository.findDetailById(BOOKING_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getDetailBooking(BOOKING_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_FOUND);
    }

    // ─── 헬퍼 ──────────────────────────────────────

    private Booking booking(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getNickname()).willReturn("강이");
        given(user.getEmail()).willReturn("test@test.com");

        Event event = mock(Event.class);
        given(event.getTitle()).willReturn("LOL 챔피언스 코리아 결승");

        Team home = mock(Team.class); given(home.getName()).willReturn("T1");
        Team away = mock(Team.class); given(away.getName()).willReturn("KT");

        Match match = Match.builder()
                .event(event)
                .homeTeam(home)
                .awayTeam(away)
                .roundLabel("1경기")
                .matchDate(LocalDate.of(2026, 5, 25))
                .startAt(LocalDateTime.of(2026, 5, 25, 18, 30))
                .endAt(LocalDateTime.of(2026, 5, 25, 22, 30))
                .status(MatchStatus.SCHEDULED)
                .build();
        ReflectionTestUtils.setField(match, "id", 101L);

        Booking booking = Booking.create(user, match, "B202605250001", 450000);
        booking.confirm();
        ReflectionTestUtils.setField(booking, "id", id);
        return booking;
    }
}
