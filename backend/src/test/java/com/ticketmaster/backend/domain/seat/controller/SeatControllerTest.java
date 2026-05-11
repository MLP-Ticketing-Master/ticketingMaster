package com.ticketmaster.backend.domain.seat.controller;

import com.ticketmaster.backend.domain.seat.dto.response.SeatSectionListResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SectionSeatListResponse;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.service.SeatService;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatController.class)
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeatService seatService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("1단계 정상 응답 (200) — 구역 목록 + 등급별 잔여 JSON 포맷")
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
    @DisplayName("1단계 비로그인 → 401 (인증 필수)")
    void 구역목록_조회_비로그인() throws Exception {
        // given: @WithMockUser 없음 → 비인증 요청

        // when & then
        mockMvc.perform(get("/matches/10/sections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("1단계 매치 없음 → 404 MATCH_NOT_FOUND")
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
    @DisplayName("2단계 정상 응답 (200) — 좌석 목록 JSON 포맷")
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
    @DisplayName("2단계 비로그인 → 401")
    void 좌석목록_조회_비로그인() throws Exception {
        // given: @WithMockUser 없음 → 비인증 요청

        // when & then
        mockMvc.perform(get("/matches/10/sections/101/seats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("2단계 구역 없음 → 404 SECTION_NOT_FOUND")
    void 좌석목록_조회_구역없음() throws Exception {
        // given
        given(seatService.findSeatsBySection(10L, 999L))
                .willThrow(new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/matches/10/sections/999/seats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SECTION_NOT_FOUND"));
    }
}