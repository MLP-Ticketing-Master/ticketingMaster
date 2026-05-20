package com.ticketmaster.backend.domain.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReleaseResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SeatReserveResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SeatSectionListResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SectionSeatListResponse;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.service.SeatReservationService;
import com.ticketmaster.backend.domain.seat.service.SeatService;
import com.ticketmaster.backend.domain.user.entity.Role;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatController.class)
@Import(SecurityConfig.class)
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private SeatService seatService;

    @MockitoBean
    private SeatReservationService seatReservationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long MATCH_ID = 10L;
    private static final Long USER_ID = 99L;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[1단계] 구역 목록 조회 - 성공 (200)")
    void 구역목록_조회_정상() throws Exception {
        // given
        SeatSectionListResponse response = SeatSectionListResponse.of(
                10L,
                List.of(
                        SeatSectionListResponse.SectionItem.of(101L, "좌측", 1, 2L),
                        SeatSectionListResponse.SectionItem.of(102L, "중앙", 2, 3L)
                ),
                List.of(
                        SeatSectionListResponse.GradeAvailability.of("VIP", "#FFD700", 2L),
                        SeatSectionListResponse.GradeAvailability.of("R",   "#C0C0C0", 3L)
                )
        );
        given(seatService.findSectionsByMatch(10L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/matches/10/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(10))
                .andExpect(jsonPath("$.sections", hasSize(2)))
                .andExpect(jsonPath("$.sections[0].sectionId").value(101))
                .andExpect(jsonPath("$.sections[0].name").value("좌측"))
                .andExpect(jsonPath("$.sections[0].availableCount").value(2))
                .andExpect(jsonPath("$.gradeAvailability", hasSize(2)))
                .andExpect(jsonPath("$.gradeAvailability[0].gradeCode").value("VIP"))
                .andExpect(jsonPath("$.gradeAvailability[0].colorHex").value("#FFD700"));
    }

    @Test
    @DisplayName("[1단계] 구역 목록 조회 - 비로그인 (401)")
    void 구역목록_조회_비로그인() throws Exception {
        // given: @WithMockUser 없음 → 비인증 요청

        // when & then
        mockMvc.perform(get("/matches/10/sections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[1단계] 구역 목록 조회 - 매치 없음 (404)")
    void 구역목록_조회_매치없음() throws Exception {
        // given
        given(seatService.findSectionsByMatch(999L))
                .willThrow(new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/matches/999/sections"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[2단계] 좌석 목록 조회 - 성공 (200)")
    void 좌석목록_조회_정상() throws Exception {
        // given
        SeatGrade grade = SeatGrade.create(null, "VIP", 100000, "#FFD700");
        Seat seat = Seat.create(null, null, grade, "A", 1, "VIP-A-1");
        ReflectionTestUtils.setField(seat, "id", 1L);
        ReflectionTestUtils.setField(seat, "status", SeatStatus.AVAILABLE);

        SectionSeatListResponse response = SectionSeatListResponse.of(
                10L, 101L, "좌측",
                List.of(SectionSeatListResponse.SeatItem.from(seat))
        );
        given(seatService.findSeatsBySection(10L, 101L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/matches/10/sections/101/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(10))
                .andExpect(jsonPath("$.sectionId").value(101))
                .andExpect(jsonPath("$.sectionName").value("좌측"))
                .andExpect(jsonPath("$.seats", hasSize(1)))
                .andExpect(jsonPath("$.seats[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.seats[0].gradeCode").value("VIP"));
    }

    @Test
    @DisplayName("[2단계] 좌석 목록 조회 - 비로그인 (401)")
    void 좌석목록_조회_비로그인() throws Exception {
        // given: @WithMockUser 없음 → 비인증 요청

        // when & then
        mockMvc.perform(get("/matches/10/sections/101/seats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("[2단계] 좌석 목록 조회 - 구역 없음 (404)")
    void 좌석목록_조회_구역없음() throws Exception {
        // given
        given(seatService.findSeatsBySection(10L, 999L))
                .willThrow(new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/matches/10/sections/999/seats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SECTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("좌석 점유 - 성공 (200)")
    void 점유_정상() throws Exception {
        // given
        SeatReserveResponse response = SeatReserveResponse.of(
                List.of(1L, 2L), LocalDateTime.of(2026, 5, 11, 18, 0), 200_000);
        given(seatReservationService.reserve(eq(MATCH_ID), eq(USER_ID), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/matches/{matchId}/seats/reserve", MATCH_ID)
                        .with(csrf())
                        .with(authentication(userAuth(USER_ID)))
                        .header("Queue-Token", "test-queue-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seatIds", List.of(1, 2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedSeatIds.length()").value(2))
                .andExpect(jsonPath("$.reservedSeatIds[0]").value(1))
                .andExpect(jsonPath("$.totalPrice").value(200000));
    }

    @Test
    @DisplayName("좌석 점유 - 충돌 (409)")
    void 점유_충돌() throws Exception {
        // given — 서비스가 conflictSeatIds 가 포함된 BusinessException 던짐
        given(seatReservationService.reserve(eq(MATCH_ID), eq(USER_ID), any()))
                .willThrow(new BusinessException(
                        ErrorCode.SEAT_ALREADY_RESERVED, List.of(2L)));

        // when & then
        mockMvc.perform(post("/matches/{matchId}/seats/reserve", MATCH_ID)
                        .with(csrf())
                        .with(authentication(userAuth(USER_ID)))
                        .header("Queue-Token", "test-queue-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seatIds", List.of(1, 2)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_ALREADY_RESERVED"))
                .andExpect(jsonPath("$.conflictSeatIds.length()").value(1))
                .andExpect(jsonPath("$.conflictSeatIds[0]").value(2));
    }

    @Test
    @DisplayName("좌석 점유 - 비로그인 (401)")
    void 점유_비로그인() throws Exception {
        // given: authentication() 없음

        // when & then
        mockMvc.perform(post("/matches/{matchId}/seats/reserve", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seatIds", List.of(1)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("좌석 점유 - 빈 배열 (400)")
    void 점유_검증실패() throws Exception {
        // when & then
        mockMvc.perform(post("/matches/{matchId}/seats/reserve", MATCH_ID)
                        .with(csrf())
                        .with(authentication(userAuth(USER_ID)))
                        .header("Queue-Token", "test-queue-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seatIds", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("좌석 해제 - 성공 (200)")
    void 해제_정상() throws Exception {
        // given
        SeatReleaseResponse response = SeatReleaseResponse.of(List.of(1L, 2L));
        given(seatReservationService.release(eq(MATCH_ID), eq(USER_ID), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(delete("/matches/{matchId}/seats/reserve", MATCH_ID)
                        .with(csrf())
                        .with(authentication(userAuth(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seatIds", List.of(1, 2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releasedSeatIds.length()").value(2))
                .andExpect(jsonPath("$.releasedSeatIds[0]").value(1));
    }

    @Test
    @DisplayName("좌석 해제 - 비로그인 (401)")
    void 해제_비로그인() throws Exception {
        // when & then
        mockMvc.perform(delete("/matches/{matchId}/seats/reserve", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seatIds", List.of(1)))))
                .andExpect(status().isUnauthorized());
    }

    // ----- 헬퍼 --------------------------------------------------------------

    /** @AuthenticationPrincipal CustomUserDetails 주입용 — @WithMockUser 로는 NPE */
    private Authentication userAuth(Long userId) {
        User user = BeanUtils.instantiateClass(User.class);  // protected 생성자 통과
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "email", "u" + userId + "@test.com");
        ReflectionTestUtils.setField(user, "role", Role.USER);

        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
    }
}