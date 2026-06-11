package com.ticketmaster.backend.admin.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.admin.team.dto.response.AdminTeamResponse;
import com.ticketmaster.backend.admin.team.service.AdminTeamService;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTeamController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminTeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private AdminTeamService service;

    // JWT 머지 후 JwtAuthenticationFilter(@Component)가 슬라이스 테스트에 자동 스캔됨
    // → 그 안의 JwtTokenProvider/CustomUserDetailsService를 mock으로 채워야 컨텍스트 로드 성공
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long TEAM_ID = 1L;

    @Test
    @DisplayName("팀_목록조회_필터없음_200")
    void 팀_목록조회_200() throws Exception {
        // given
        given(service.getTeams(null))
                .willReturn(List.of(
                        response(1L, "T1", SportType.LOL),
                        response(2L, "Gen.G", SportType.LOL)
                ));

        // when & then
        mockMvc.perform(get("/admin/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("T1"))
                .andExpect(jsonPath("$[1].name").value("Gen.G"));
    }

    @Test
    @DisplayName("팀_목록조회_종목필터_200")
    void 팀_목록조회_종목필터_200() throws Exception {
        // given
        given(service.getTeams(SportType.LOL))
                .willReturn(List.of(response(1L, "T1", SportType.LOL)));

        // when & then
        mockMvc.perform(get("/admin/teams").param("sportType", "LOL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sportType").value("LOL"));
    }

    @Test
    @DisplayName("팀_목록조회_빈결과_200")
    void 팀_목록조회_빈결과_200() throws Exception {
        // given
        given(service.getTeams(any())).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/admin/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("팀_등록_정상_201")
    void 팀_등록_201() throws Exception {
        // given
        given(service.createTeam(any()))
                .willReturn(response(TEAM_ID, "T1", SportType.LOL));

        // when & then
        mockMvc.perform(post("/admin/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "T1",
                                "sportType", "LOL",
                                "logoImageUrl", "https://example.com/t1.png"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teamId").value(TEAM_ID))
                .andExpect(jsonPath("$.name").value("T1"));
    }

    @Test
    @DisplayName("등록_검증실패_name_빈값_400")
    void 팀_등록_이름비어있음_400() throws Exception {
        // when & then — name 빈 문자열 → @NotBlank 위반
        mockMvc.perform(post("/admin/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "", "sportType", "LOL"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("등록_검증실패_sportType_누락_400")
    void 팀_등록_종목누락_400() throws Exception {
        // when & then — sportType 누락 → @NotNull 위반
        // Map.of()는 null 값 허용 안 하므로 HashMap 사용
        Map<String, Object> body = new HashMap<>();
        body.put("name", "T1");
        body.put("sportType", null);

        mockMvc.perform(post("/admin/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("등록_DUPLICATE_TEAM_NAME_409")
    void 팀_등록_중복_409() throws Exception {
        // given
        given(service.createTeam(any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME));

        // when & then
        mockMvc.perform(post("/admin/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "T1", "sportType", "LOL"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TEAM_NAME"));
    }


    @Test
    @DisplayName("팀_수정_정상_200")
    void 팀_수정_200() throws Exception {
        // given
        given(service.updateTeam(eq(TEAM_ID), any()))
                .willReturn(response(TEAM_ID, "T1 Esports", SportType.LOL));

        // when & then
        mockMvc.perform(patch("/admin/teams/{id}", TEAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "T1 Esports"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T1 Esports"));
    }

    @Test
    @DisplayName("팀_수정_없음_404")
    void 팀_수정_없음_404() throws Exception {
        // given
        given(service.updateTeam(eq(TEAM_ID), any()))
                .willThrow(new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/admin/teams/{id}", TEAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "T1 Esports"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));
    }

    @Test
    @DisplayName("팀_수정_이름중복_409")
    void 팀_수정_중복_409() throws Exception {
        // given
        given(service.updateTeam(eq(TEAM_ID), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME));

        // when & then
        mockMvc.perform(patch("/admin/teams/{id}", TEAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Gen.G"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TEAM_NAME"));
    }


    @Test
    @DisplayName("팀_삭제_정상_204")
    void 팀_삭제_204() throws Exception {
        // given
        willDoNothing().given(service).deleteTeam(TEAM_ID);

        // when
        mockMvc.perform(delete("/admin/teams/{id}", TEAM_ID).with(csrf()))
                .andExpect(status().isNoContent());

        // then
        verify(service).deleteTeam(TEAM_ID);
    }

    @Test
    @DisplayName("팀_삭제_없음_404")
    void 팀_삭제_없음_404() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.TEAM_NOT_FOUND))
                .given(service).deleteTeam(TEAM_ID);

        // when & then
        mockMvc.perform(delete("/admin/teams/{id}", TEAM_ID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ADMIN_권한_없음_403")
    void 권한없음_403() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/teams"))
                .andExpect(status().isForbidden());
    }

    // ─── 헬퍼 (응답 DTO 는 진짜 엔티티에서 from()) ──────

    private AdminTeamResponse response(Long id, String name, SportType sportType) {
        Team team = Team.builder()
                .name(name)
                .sportType(sportType)
                .logoImageUrl("https://example.com/logo.png")
                .build();
        ReflectionTestUtils.setField(team, "id", id);
        return AdminTeamResponse.from(team);
    }
}
