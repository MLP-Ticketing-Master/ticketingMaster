package com.ticketmaster.backend.admin.seatgrade.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.admin.seatgrade.dto.response.AdminSeatGradeResponse;
import com.ticketmaster.backend.admin.seatgrade.service.AdminSeatGradeService;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

@WebMvcTest(AdminSeatGradeController.class)
@WithMockUser(roles = "ADMIN")
class AdminSeatGradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private AdminSeatGradeService service;

    private static final Long EVENT_ID = 1L;
    private static final Long GRADE_ID = 10L;

    @Test
    @DisplayName("등급_목록조회_200")
    void 좌석등급_목록조회_200() throws Exception {
        // given
        given(service.findAllByEvent(EVENT_ID))
                .willReturn(List.of(response(GRADE_ID, "VIP", 200000, "#FF0000")));

        // when & then
        mockMvc.perform(get("/admin/events/{eventId}/seat-grades", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].gradeCode").value("VIP"))
                .andExpect(jsonPath("$[0].price").value(200000));
    }

    @Test
    @DisplayName("등급_등록_정상_201")
    void 좌석등급_등록_201() throws Exception {
        // given
        given(service.create(eq(EVENT_ID), any()))
                .willReturn(response(GRADE_ID, "VIP", 100000, "#FF0000"));

        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/seat-grades", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "gradeCode", "VIP",
                                "price", 100000,
                                "colorHex", "#FF0000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatGradeId").value(GRADE_ID))
                .andExpect(jsonPath("$.gradeCode").value("VIP"));
    }

    @Test
    @DisplayName("등록_검증실패_gradeCode_빈값_400")
    void 좌석등급_등록_코드비어있음_400() throws Exception {
        // when & then — gradeCode 빈 문자열 → @NotBlank 위반
        mockMvc.perform(post("/admin/events/{eventId}/seat-grades", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "gradeCode", "", "price", 100000, "colorHex", "#FF0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("등록_검증실패_price_음수_400")
    void 좌석등급_등록_가격음수_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/seat-grades", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "gradeCode", "VIP", "price", -1, "colorHex", "#FF0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("등록_검증실패_colorHex_형식_400")
    void 좌석등급_등록_색상형식_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/seat-grades", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "gradeCode", "VIP", "price", 100000, "colorHex", "not-hex"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("등록_DUPLICATE_GRADE_CODE_409")
    void 좌석등급_등록_중복_409() throws Exception {
        // given
        given(service.create(eq(EVENT_ID), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_GRADE_CODE));

        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/seat-grades", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "gradeCode", "VIP", "price", 100000, "colorHex", "#FF0000"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_GRADE_CODE"));
    }

    @Test
    @DisplayName("등록_EVENT_NOT_FOUND_404")
    void 좌석등급_등록_대회없음_404() throws Exception {
        // given
        given(service.create(eq(EVENT_ID), any()))
                .willThrow(new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/admin/events/{eventId}/seat-grades", EVENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "gradeCode", "VIP", "price", 100000, "colorHex", "#FF0000"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("등급_수정_정상_200")
    void 좌석등급_수정_200() throws Exception {
        // given
        given(service.update(eq(GRADE_ID), any()))
                .willReturn(response(GRADE_ID, "VIP", 150000, "#00FF00"));

        // when & then
        mockMvc.perform(patch("/admin/seat-grades/{id}", GRADE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("price", 150000, "colorHex", "#00FF00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(150000));
    }

    @Test
    @DisplayName("등급_수정_없음_404")
    void 좌석등급_수정_없음_404() throws Exception {
        // given
        given(service.update(eq(GRADE_ID), any()))
                .willThrow(new BusinessException(ErrorCode.SEAT_GRADE_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/admin/seat-grades/{id}", GRADE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("price", 150000))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEAT_GRADE_NOT_FOUND"));
    }

    @Test
    @DisplayName("등급_삭제_정상_204")
    void 좌석등급_삭제_204() throws Exception {
        // given
        willDoNothing().given(service).delete(GRADE_ID);

        // when
        mockMvc.perform(delete("/admin/seat-grades/{id}", GRADE_ID).with(csrf()))
                .andExpect(status().isNoContent());

        // then
        verify(service).delete(GRADE_ID);
    }

    @Test
    @DisplayName("등급_삭제_사용중_409")
    void 좌석등급_삭제_사용중_409() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.SEAT_GRADE_IN_USE))
                .given(service).delete(GRADE_ID);

        // when & then
        mockMvc.perform(delete("/admin/seat-grades/{id}", GRADE_ID).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_GRADE_IN_USE"));
    }

    @Test
    @Disabled("SecurityConfig (@EnableMethodSecurity) 머지 후 활성화")
    @WithMockUser(roles = "USER")
    @DisplayName("ADMIN_권한_없음_403")
    void 권한없음_403() throws Exception {
        // when & then
        mockMvc.perform(get("/admin/events/{eventId}/seat-grades", EVENT_ID))
                .andExpect(status().isForbidden());
    }

    // ─── 헬퍼 (응답 DTO 는 진짜 엔티티에서 from()) ──────

    private AdminSeatGradeResponse response(Long id, String code, Integer price, String hex) {
        SeatGrade g = SeatGrade.create(mock(Event.class), code, price, hex);
        ReflectionTestUtils.setField(g, "id", id);
        return AdminSeatGradeResponse.from(g);
    }

}