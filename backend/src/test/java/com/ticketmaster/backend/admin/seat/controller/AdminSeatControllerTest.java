package com.ticketmaster.backend.admin.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.admin.seat.dto.response.AdminSeatResponse;
import com.ticketmaster.backend.admin.seat.service.AdminSeatService;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSeatController.class)
@WithMockUser(roles = "ADMIN")
class AdminSeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private AdminSeatService service;

    private static final Long MATCH_ID = 1L;
    private static final Long SEAT_ID = 500L;

    // ─── 단건 등록 ─────────────────────────────────────

    @Test
    @DisplayName("좌석_단건등록_정상_201")
    void 좌석_등록_201() throws Exception {
        // given
        given(service.create(eq(MATCH_ID), any()))
                .willReturn(response(SEAT_ID, "VIP-A-1", "좌측", "VIP", 100000, SeatStatus.AVAILABLE));

        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "sectionId", 10, "seatGradeId", 20,
                                "rowLabel", "A", "seatNo", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatId").value(SEAT_ID))
                .andExpect(jsonPath("$.seatCode").value("VIP-A-1"))
                .andExpect(jsonPath("$.sectionName").value("좌측"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("좌석_등록_검증실패_sectionId_누락_400")
    void 좌석_등록_구역누락_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "seatGradeId", 20,
                                "rowLabel", "A", "seatNo", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("좌석_등록_검증실패_rowLabel_빈값_400")
    void 좌석_등록_행라벨비어있음_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "sectionId", 10, "seatGradeId", 20,
                                "rowLabel", "", "seatNo", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("좌석_등록_검증실패_seatNo_0이하_400")
    void 좌석_등록_좌석번호_0이하_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "sectionId", 10, "seatGradeId", 20,
                                "rowLabel", "A", "seatNo", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("좌석_등록_DUPLICATE_SEAT_CODE_409")
    void 좌석_등록_코드중복_409() throws Exception {
        // given
        given(service.create(eq(MATCH_ID), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE));

        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "sectionId", 10, "seatGradeId", 20,
                                "rowLabel", "A", "seatNo", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SEAT_CODE"));
    }

    @Test
    @DisplayName("좌석_등록_MATCH_NOT_FOUND_404")
    void 좌석_등록_회차없음_404() throws Exception {
        // given
        given(service.create(eq(MATCH_ID), any()))
                .willThrow(new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "sectionId", 10, "seatGradeId", 20,
                                "rowLabel", "A", "seatNo", 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    // ─── 일괄 등록 ─────────────────────────────────────

    @Test
    @DisplayName("좌석_일괄등록_정상_201")
    void 좌석_일괄등록_201() throws Exception {
        // given
        given(service.bulkCreate(eq(MATCH_ID), any()))
                .willReturn(List.of(
                        response(1L, "VIP-A-1", "좌측", "VIP", 100000, SeatStatus.AVAILABLE),
                        response(2L, "VIP-A-2", "좌측", "VIP", 100000, SeatStatus.AVAILABLE)));
        String body = om.writeValueAsString(Map.of("seats", List.of(
                Map.of("sectionId", 10, "seatGradeId", 20, "rowLabel", "A", "seatNo", 1),
                Map.of("sectionId", 10, "seatGradeId", 20, "rowLabel", "A", "seatNo", 2))));

        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats/bulk", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("좌석_일괄등록_빈배열_400_NotEmpty")
    void 좌석_일괄등록_빈배열_400() throws Exception {
        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats/bulk", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seats", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("좌석_일괄등록_5001개_400_Size_max")
    void 좌석_일괄등록_상한초과_400() throws Exception {
        // given — 5001개 좌석 페이로드
        List<Map<String, Object>> seats = IntStream.rangeClosed(1, 5001)
                .mapToObj(i -> Map.<String, Object>of(
                        "sectionId", 10, "seatGradeId", 20,
                        "rowLabel", "A", "seatNo", i))
                .toList();

        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats/bulk", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seats", seats))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("좌석_일괄등록_DUPLICATE_SEAT_CODE_409")
    void 좌석_일괄등록_중복_409() throws Exception {
        // given
        given(service.bulkCreate(eq(MATCH_ID), any()))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE));
        String body = om.writeValueAsString(Map.of("seats", List.of(
                Map.of("sectionId", 10, "seatGradeId", 20, "rowLabel", "A", "seatNo", 1))));

        // when & then
        mockMvc.perform(post("/admin/matches/{id}/seats/bulk", MATCH_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SEAT_CODE"));
    }

    // ─── 조회 ─────────────────────────────────────────

    @Test
    @DisplayName("좌석_전체조회_200")
    void 좌석_조회_200() throws Exception {
        // given
        given(service.findAllByMatch(MATCH_ID))
                .willReturn(List.of(response(SEAT_ID, "VIP-A-1", "좌측", "VIP", 100_000, SeatStatus.AVAILABLE)));

        // when & then
        mockMvc.perform(get("/admin/matches/{id}/seats", MATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].seatCode").value("VIP-A-1"))
                .andExpect(jsonPath("$[0].sectionName").value("좌측"));
    }

    // ─── 수정 ─────────────────────────────────────────

    @Test
    @DisplayName("좌석_수정_200")
    void 좌석_수정_200() throws Exception {
        // given
        given(service.update(eq(SEAT_ID), any()))
                .willReturn(response(SEAT_ID, "VIP-A-1", "중앙", "R", 80000, SeatStatus.AVAILABLE));

        // when & then
        mockMvc.perform(patch("/admin/seats/{id}", SEAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("sectionId", 11, "seatGradeId", 21))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionName").value("중앙"))
                .andExpect(jsonPath("$.gradeCode").value("R"));
    }

    @Test
    @DisplayName("좌석_수정_SEAT_NOT_EDITABLE_409")
    void 좌석_수정_편집불가_409() throws Exception {
        // given
        given(service.update(eq(SEAT_ID), any()))
                .willThrow(new BusinessException(ErrorCode.SEAT_NOT_EDITABLE));

        // when & then
        mockMvc.perform(patch("/admin/seats/{id}", SEAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("seatGradeId", 21))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_NOT_EDITABLE"));
    }

    // ─── 삭제 ─────────────────────────────────────────

    @Test
    @DisplayName("좌석_삭제_204")
    void 좌석_삭제_204() throws Exception {
        // given
        willDoNothing().given(service).delete(SEAT_ID);

        // when
        mockMvc.perform(delete("/admin/seats/{id}", SEAT_ID).with(csrf()))
                .andExpect(status().isNoContent());

        // then
        verify(service).delete(SEAT_ID);
    }

    @Test
    @DisplayName("좌석_삭제_SEAT_NOT_DELETABLE_409")
    void 좌석_삭제_불가_409() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.SEAT_NOT_DELETABLE))
                .given(service).delete(SEAT_ID);

        // when & then
        mockMvc.perform(delete("/admin/seats/{id}", SEAT_ID).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_NOT_DELETABLE"));
    }

    // ─── 헬퍼 ─────────────────────────────────────────

    private AdminSeatResponse response(Long id, String code, String sectionName,
                                       String gradeCode, Integer price, SeatStatus status) {
        Event event = mock(Event.class);
        Section sec = Section.create(event, sectionName, 1, "");
        ReflectionTestUtils.setField(sec, "id", 10L);
        SeatGrade grade = SeatGrade.create(event, gradeCode, price, "#FF0000");
        ReflectionTestUtils.setField(grade, "id", 20L);
        Seat seat = Seat.create(null, sec, grade, "A", 1, code);
        ReflectionTestUtils.setField(seat, "id", id);
        if (status != SeatStatus.AVAILABLE) {
            ReflectionTestUtils.setField(seat, "status", status);
        }
        return AdminSeatResponse.from(seat);
    }

}