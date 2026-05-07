package com.ticketmaster.backend.admin.seatgrade.service;

import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeCreateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.request.AdminSeatGradeUpdateRequest;
import com.ticketmaster.backend.admin.seatgrade.dto.response.AdminSeatGradeResponse;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminSeatGradeServiceTest {

    @InjectMocks
    private AdminSeatGradeService service;

    @Mock
    private SeatGradeRepository seatGradeRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private EventRepository eventRepository;

    private static final Long EVENT_ID = 1L;
    private static final Long GRADE_ID = 10L;

    private Event event;
    private SeatGrade vipGrade;       // 활성
    private SeatGrade rGrade;         // 활성, 다른 코드
    private SeatGrade deletedGrade;   // soft-deleted

    @BeforeEach
    void setUp() {
        event = mock(Event.class);

        vipGrade = SeatGrade.create(event, "VIP", 200000, "#FF0000");
        ReflectionTestUtils.setField(vipGrade, "id", GRADE_ID);

        rGrade = SeatGrade.create(event, "R", 100_000, "#00FF00");
        ReflectionTestUtils.setField(rGrade, "id", 11L);

        deletedGrade = SeatGrade.create(event, "VIP", 80_000, "#000000");
        ReflectionTestUtils.setField(deletedGrade, "id", 99L);
        deletedGrade.softDelete();
    }

    // ─── 전체 조회 ───────────────────────────────────

    @Test
    @DisplayName("좌석등급_목록을_가격_내림차순으로_반환")
    void 좌석등급_목록조회_성공() {
        // given
        given(eventRepository.existsById(EVENT_ID)).willReturn(true);
        given(seatGradeRepository.findAllByEventIdOrderByPriceDesc(EVENT_ID))
                .willReturn(List.of(vipGrade, rGrade));

        // when
        List<AdminSeatGradeResponse> result = service.findAllByEvent(EVENT_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getGradeCode()).isEqualTo("VIP");
        assertThat(result.get(0).getPrice()).isEqualTo(200000);
        assertThat(result.get(1).getGradeCode()).isEqualTo("R");
    }

    @Test
    @DisplayName("존재하지_않는_event_조회시_EVENT_NOT_FOUND")
    void 좌석등급_목록조회_대회없음() {
        // given
        given(eventRepository.existsById(EVENT_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> service.findAllByEvent(EVENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVENT_NOT_FOUND);
        verify(seatGradeRepository, never()).findAllByEventIdOrderByPriceDesc(any());
    }

    // ─── create ─────────────────────────────────────────────

    @Test
    @DisplayName("신규_좌석등급_등록_성공")
    void 좌석등급_등록_신규() {
        // given
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
        given(seatGradeRepository.findByEventIdAndGradeCodeIncludingDeleted(EVENT_ID, "S"))
                .willReturn(Optional.empty());
        // save() 호출 시 ID 를 박아서 반환 (JPA 동작 흉내)
        given(seatGradeRepository.save(any(SeatGrade.class)))
                .willAnswer(inv -> {
                    SeatGrade g = inv.getArgument(0);
                    ReflectionTestUtils.setField(g, "id", 50L);
                    return g;
                });

        // when
        AdminSeatGradeResponse res = service.create(EVENT_ID, createReq("S", 50000, "#0000FF"));

        // then
        assertThat(res.getSeatGradeId()).isEqualTo(50L);
        assertThat(res.getGradeCode()).isEqualTo("S");
        assertThat(res.getPrice()).isEqualTo(50000);
        assertThat(res.getColorHex()).isEqualTo("#0000FF");
        // save 에 넘긴 entity 의 필드도 체크 — 진짜 SeatGrade.create 가 동작했는지
        ArgumentCaptor<SeatGrade> captor = ArgumentCaptor.forClass(SeatGrade.class);
        verify(seatGradeRepository).save(captor.capture());
        assertThat(captor.getValue().getGradeCode()).isEqualTo("S");
        assertThat(captor.getValue().getEvent()).isSameAs(event);
    }

    @Test
    @DisplayName("활성_상태의_동일_코드_존재시_DUPLICATE_GRADE_CODE")
    void 좌석등급_등록_활성중복() {
        // given
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
        given(seatGradeRepository.findByEventIdAndGradeCodeIncludingDeleted(EVENT_ID, "VIP"))
                .willReturn(Optional.of(vipGrade));   // 활성 상태

        // when & then
        assertThatThrownBy(() -> service.create(EVENT_ID, createReq("VIP", 100000, "#FF0000")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_GRADE_CODE);
        verify(seatGradeRepository, never()).save(any());
        assertThat(vipGrade.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된_동일_코드_존재시_복구하고_가격_색상_갱신")
    void 좌석등급_등록_복구() {
        // given
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
        given(seatGradeRepository.findByEventIdAndGradeCodeIncludingDeleted(EVENT_ID, "VIP"))
                .willReturn(Optional.of(deletedGrade));

        // when
        AdminSeatGradeResponse res = service.create(EVENT_ID, createReq("VIP", 120000, "#FFFFFF"));

        // then — 진짜 엔티티 상태 검증: restore() 와 update() 의 실제 동작
        assertThat(deletedGrade.isDeleted()).isFalse();
        assertThat(deletedGrade.getPrice()).isEqualTo(120000);
        assertThat(deletedGrade.getColorHex()).isEqualTo("#FFFFFF");
        assertThat(res.getSeatGradeId()).isEqualTo(99L);
        verify(seatGradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("등록시_event_존재하지_않으면_EVENT_NOT_FOUND")
    void 좌석등급_등록_대회없음() {
        // given
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.create(EVENT_ID, createReq("VIP", 100_000, "#FF0000")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVENT_NOT_FOUND);
    }

    // ─── update ─────────────────────────────────────────────

    @Test
    @DisplayName("좌석등급_부분수정_정상")
    void 좌석등급_수정_성공() {
        // given
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(vipGrade));

        // when
        AdminSeatGradeResponse res = service.update(GRADE_ID, updateReq(150000, "#00FF00"));

        // then — 진짜 엔티티 상태: update() 가 실제로 적용됨
        assertThat(vipGrade.getPrice()).isEqualTo(150000);
        assertThat(vipGrade.getColorHex()).isEqualTo("#00FF00");
        assertThat(res.getPrice()).isEqualTo(150000);
    }

    @Test
    @DisplayName("좌석등급_부분수정_price만_변경시_colorHex_유지")
    void 좌석등급_수정_부분() {
        // given
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(vipGrade));

        // when
        service.update(GRADE_ID, updateReq(150000, null));

        // then - SeatGrade.update 의 null-safe 동작 검증
        assertThat(vipGrade.getPrice()).isEqualTo(150000);
        assertThat(vipGrade.getColorHex()).isEqualTo("#FF0000");
    }

    @Test
    @DisplayName("수정_대상이_없으면_SEAT_GRADE_NOT_FOUND")
    void 좌석등급_수정_없음() {
        // given
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.update(GRADE_ID, updateReq(150000, "#00FF00")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_GRADE_NOT_FOUND);
    }

    // ─── delete ─────────────────────────────────────────────

    @Test
    @DisplayName("좌석에_배정되지_않은_등급은_softDelete")
    void 좌석등급_삭제_성공() {
        // given
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(vipGrade));
        given(seatRepository.existsBySeatGradeId(GRADE_ID)).willReturn(false);

        // when
        service.delete(GRADE_ID);

        // then - 진짜 엔티티 상태 : softDelete() 가 deletedAt 채움
        assertThat(vipGrade.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("좌석에_배정된_등급은_SEAT_GRADE_IN_USE")
    void 좌석등급_삭제_사용중() {
        // given
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(vipGrade));
        given(seatRepository.existsBySeatGradeId(GRADE_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.delete(GRADE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_GRADE_IN_USE);
        assertThat(vipGrade.isDeleted()).isFalse(); // 삭제되지 않음
    }

    @Test
    @DisplayName("삭제_대상이_없으면_SEAT_GRADE_NOT_FOUND")
    void 좌석등급_삭제_없음() {
        // given
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.delete(GRADE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_GRADE_NOT_FOUND);
    }

    // ─── 헬퍼 (요청 DTO 만 mock) ──────────────────────────

    private AdminSeatGradeCreateRequest createReq(String code, Integer price, String hex) {
        AdminSeatGradeCreateRequest req = mock(AdminSeatGradeCreateRequest.class);
        given(req.getGradeCode()).willReturn(code);
        given(req.getPrice()).willReturn(price);
        given(req.getColorHex()).willReturn(hex);
        return req;
    }

    private AdminSeatGradeUpdateRequest updateReq(Integer price, String hex) {
        AdminSeatGradeUpdateRequest req = mock(AdminSeatGradeUpdateRequest.class);
        given(req.getPrice()).willReturn(price);
        given(req.getColorHex()).willReturn(hex);
        return req;
    }



}