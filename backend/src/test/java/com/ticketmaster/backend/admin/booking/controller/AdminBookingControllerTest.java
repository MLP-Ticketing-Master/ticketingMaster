package com.ticketmaster.backend.admin.booking.controller;

import com.ticketmaster.backend.admin.booking.dto.response.AdminBookingDetailResponse;
import com.ticketmaster.backend.admin.booking.dto.response.AdminBookingListResponse;
import com.ticketmaster.backend.admin.booking.service.AdminBookingService;
import com.ticketmaster.backend.domain.booking.entity.Booking;
import com.ticketmaster.backend.domain.booking.entity.BookingStatus;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBookingController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminBookingService service;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long BOOKING_ID = 10L;

    // ─── 목록 조회 ──────────────────────────────────

    @Test
    @DisplayName("예매_목록조회_status없음_200")
    void 예매_목록조회_200() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<AdminBookingListResponse> page = new PageImpl<>(
                List.of(AdminBookingListResponse.from(booking(BOOKING_ID))), pageable, 1);
        given(service.getAllListBooking(eq(null), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].bookingId").value(BOOKING_ID))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].eventTitle").value("LOL 챔피언스 코리아 결승"));
    }

    @Test
    @DisplayName("예매_목록조회_status필터_200")
    void 예매_목록조회_status필터_200() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<AdminBookingListResponse> page = new PageImpl<>(
                List.of(AdminBookingListResponse.from(booking(BOOKING_ID))), pageable, 1);
        given(service.getAllListBooking(eq(BookingStatus.CONFIRMED), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/admin/bookings").param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("예매_목록조회_빈결과_200")
    void 예매_목록조회_빈결과_200() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        given(service.getAllListBooking(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when & then
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ─── 상세 조회 ──────────────────────────────────

    @Test
    @DisplayName("예매_상세조회_200")
    void 예매_상세조회_200() throws Exception {
        // given
        AdminBookingDetailResponse detail = AdminBookingDetailResponse.from(booking(BOOKING_ID));
        given(service.getDetailBooking(BOOKING_ID)).willReturn(detail);

        // when & then
        mockMvc.perform(get("/admin/bookings/{id}", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(BOOKING_ID))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.matchInfo.homeTeamName").value("T1"))
                .andExpect(jsonPath("$.matchInfo.awayTeamName").value("KT"));
    }

    @Test
    @DisplayName("예매_상세조회_없음_404")
    void 예매_상세조회_없음_404() throws Exception {
        // given
        given(service.getDetailBooking(BOOKING_ID))
                .willThrow(new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/admin/bookings/{id}", BOOKING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ADMIN_권한_없음_403")
    void 권한없음_403() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isForbidden());
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
