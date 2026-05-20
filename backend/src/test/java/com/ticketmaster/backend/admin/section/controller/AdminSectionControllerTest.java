package com.ticketmaster.backend.admin.section.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.admin.section.dto.response.AdminSectionResponse;
import com.ticketmaster.backend.admin.section.service.AdminSectionService;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.seat.entity.Section;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSectionController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminSectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private AdminSectionService service;

    // JWT 머지 후 JwtAuthenticationFilter(@Component)가 슬라이스 테스트에 자동 스캔됨
    // → 그 안의 JwtTokenProvider/CustomUserDetailsService를 mock으로 채워야 컨텍스트 로드 성공
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long EVENT_ID = 1L;
    private static final Long SECTION_ID = 100L;

    @Test
    @DisplayName("구역_목록조회_200")
    void 구역_목록조회_200() throws Exception {
        // given
        given(service.findAllByEvent(EVENT_ID))
                .willReturn(List.of(response(SECTION_ID, "좌측", 1, "무대 좌측 스탠드")));

        // when & then
        mockMvc.perform(get("/admin/events/{eventId}/sections", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("좌측"));
    }

    @Test
    @DisplayName("구역_등록_정상_201")
    void 구역_등록_201() throws Exception {
        // given — "우측" 신규 등록
        given(service.create(eq(EVENT_ID), any()))
                .willReturn(response(SECTION_ID, "우측", 3, "무대 우측 스탠드"));

        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/sections", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "우측",
                                "displayOrder", 3,
                                "description", "무대 우측 스탠드"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sectionId").value(SECTION_ID))
                .andExpect(jsonPath("$.name").value("우측"));
    }

    @Test
    @DisplayName("구역_등록_검증실패_name_빈값_400")
    void 구역_등록_이름비어있음_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/sections", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "",
                                "displayOrder", 3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("구역_등록_검증실패_displayOrder_음수_400")
    void 구역_등록_순서음수_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/sections", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "우측", "displayOrder", -1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구역_등록_DUPLICATE_SECTION_NAME_409")
    void 구역_등록_이름중복_409() throws Exception {
        // given
        given(service.create(eq(EVENT_ID), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_SECTION_NAME));

        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/sections", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "좌측", "displayOrder", 3))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SECTION_NAME"));
    }

    @Test
    @DisplayName("구역_등록_DUPLICATE_SECTION_DISPLAY_ORDER_409")
    void 구역_등록_순서중복_409() throws Exception {
        // given
        given(service.create(eq(EVENT_ID), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_SECTION_DISPLAY_ORDER));

        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/sections", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "우측", "displayOrder", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SECTION_DISPLAY_ORDER"));
    }

    @Test
    @DisplayName("구역_등록_EVENT_NOT_FOUND_404")
    void 구역_등록_대회없음_404() throws Exception {
        // given
        given(service.create(eq(EVENT_ID), any()))
                .willThrow(new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/sections", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "우측", "displayOrder", 3))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("구역_수정_정상_200")
    void 구역_수정_200() throws Exception {
        // given
        given(service.update(eq(SECTION_ID), any()))
                .willReturn(response(SECTION_ID, "좌측", 5, "변경"));

        // when & then
        mockMvc.perform(patch("/admin/sections/{id}", SECTION_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("displayOrder", 5, "description", "변경"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(5));
    }

    @Test
    @DisplayName("구역_수정_없음_404")
    void 구역_수정_없음_404() throws Exception {
        // given
        given(service.update(eq(SECTION_ID), any()))
                .willThrow(new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/admin/sections/{id}", SECTION_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("displayOrder", 5))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SECTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("구역_수정_name_공백문자열_400")
    void 구역_수정_공백이름_400() throws Exception {
        // when & then — Pattern '.*\\S.*' 위반
        mockMvc.perform(patch("/admin/sections/{id}", SECTION_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("구역_삭제_정상_204")
    void 구역_삭제_204() throws Exception {
        // given
        willDoNothing().given(service).delete(SECTION_ID);

        // when
        mockMvc.perform(delete("/admin/sections/{id}", SECTION_ID).with(csrf()))
                .andExpect(status().isNoContent());

        // then
        verify(service).delete(SECTION_ID);
    }

    @Test
    @DisplayName("구역_삭제_사용중_409")
    void 구역_삭제_사용중_409() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.SECTION_IN_USE))
                .given(service).delete(SECTION_ID);

        // when & then
        mockMvc.perform(delete("/admin/sections/{id}", SECTION_ID).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SECTION_IN_USE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("ADMIN_권한_없음_403")
    void 권한없음_403() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/events/{eventId}/sections", EVENT_ID))
                .andExpect(status().isForbidden());
    }

    private AdminSectionResponse response(Long id, String name, Integer order, String desc) {
        Section s = Section.create(mock(Event.class), name, order, desc);
        ReflectionTestUtils.setField(s, "id", id);
        return AdminSectionResponse.from(s);
    }





}