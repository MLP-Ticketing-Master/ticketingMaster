package com.ticketmaster.backend.admin.seat.service;

import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatBulkCreateRequest;
import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatCreateRequest;
import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatUpdateRequest;
import com.ticketmaster.backend.admin.seat.dto.response.AdminSeatResponse;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminSeatServiceTest {

    @InjectMocks
    private AdminSeatService service;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SeatGradeRepository seatGradeRepository;

    @Mock
    private MatchRepository matchRepository;

    private static final Long MATCH_ID = 1L;
    private static final Long SECTION_ID = 10L;
    private static final Long GRADE_ID = 20L;
    private static final Long SEAT_ID = 500L;

    private Event event;
    private Match match;
    private Section section;       // "좌측"
    private SeatGrade grade;       // "VIP", 100000
    private Seat availableSeat;    // status=AVAILABLE

    @BeforeEach
    void setUp() {
        event = mock(Event.class);
        match = mock(Match.class);

        section = Section.create(event, "좌측", 1, "무대 좌측 스탠드");
        ReflectionTestUtils.setField(section, "id", SECTION_ID);

        grade = SeatGrade.create(event, "VIP", 100000, "#FF0000");
        ReflectionTestUtils.setField(grade, "id", GRADE_ID);

        availableSeat = Seat.create(match, section, grade, "A", 1, "VIP-A-1");
        ReflectionTestUtils.setField(availableSeat, "id", SEAT_ID);

        // 정상 케이스 기본 stubbing — MATCH_NOT_FOUND 테스트는 개별적으로 override
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.of(match));
        given(matchRepository.existsById(MATCH_ID)).willReturn(true);
    }

    // ─── create (단건) ───────────────────────────────────

    @Test
    @DisplayName("좌석_단건등록_seatCode_자동조합_후_저장")
    void 좌석_단건등록_성공() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
        given(seatRepository.existsByMatchIdAndSectionIdAndSeatCode(MATCH_ID, SECTION_ID, "VIP-A-2")).willReturn(false);
        given(seatRepository.save(any(Seat.class)))
                .willAnswer(inv -> {
                    Seat s = inv.getArgument(0);
                    ReflectionTestUtils.setField(s, "id", 600L);
                    return s;
                });

        // when
        AdminSeatResponse res = service.create(MATCH_ID, createReq(SECTION_ID, GRADE_ID, "A", 2));

        // then
        ArgumentCaptor<Seat> captor = ArgumentCaptor.forClass(Seat.class);
        verify(seatRepository).save(captor.capture());
        Seat saved = captor.getValue();
        assertThat(saved.getSeatCode()).isEqualTo("VIP-A-2");
        assertThat(saved.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(saved.getSection()).isSameAs(section);
        assertThat(saved.getSeatGrade()).isSameAs(grade);
        assertThat(saved.getMatch()).isSameAs(match);
        assertThat(res.getSeatCode()).isEqualTo("VIP-A-2");
    }

    @Test
    @DisplayName("좌석_단건등록_같은_seatCode_존재시_DUPLICATE_SEAT_CODE")
    void 좌석_단건등록_코드중복() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.of(grade));
        given(seatRepository.existsByMatchIdAndSectionIdAndSeatCode(MATCH_ID, SECTION_ID, "VIP-A-1")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.create(MATCH_ID, createReq(SECTION_ID, GRADE_ID, "A", 1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SEAT_CODE);
        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("좌석_단건등록_match_없음_MATCH_NOT_FOUND")
    void 좌석_단건등록_회차없음() {
        // given
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.create(MATCH_ID, createReq(SECTION_ID, GRADE_ID, "A", 1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("좌석_단건등록_section_없음_SECTION_NOT_FOUND")
    void 좌석_단건등록_구역없음() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.create(MATCH_ID, createReq(SECTION_ID, GRADE_ID, "A", 1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SECTION_NOT_FOUND);
    }

    @Test
    @DisplayName("좌석_단건등록_grade_없음_SEAT_GRADE_NOT_FOUND")
    void 좌석_단건등록_등급없음() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
        given(seatGradeRepository.findById(GRADE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.create(MATCH_ID, createReq(SECTION_ID, GRADE_ID, "A", 1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_GRADE_NOT_FOUND);
    }

    // ─── bulkCreate (일괄) ─────────────────────────────────────────

    @Test
    @DisplayName("좌석_일괄등록_정상_saveAll_1회_호출")
    void 좌석_일괄등록_성공() {
        // given
        given(sectionRepository.findAllById(Set.of(SECTION_ID))).willReturn(List.of(section));
        given(seatGradeRepository.findAllById(Set.of(GRADE_ID))).willReturn(List.of(grade));
        given(seatRepository.findExistingSectionCodePairs(eq(MATCH_ID), anyCollection()))
                .willReturn(List.of());
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1),
                createReq(SECTION_ID, GRADE_ID, "A", 2),
                createReq(SECTION_ID, GRADE_ID, "A", 3)
        ));

        // when
        service.bulkCreate(MATCH_ID, req);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue()).extracting(Seat::getSeatCode)
                .containsExactly("VIP-A-1", "VIP-A-2", "VIP-A-3");
        assertThat(captor.getValue()).allSatisfy(s -> {
            assertThat(s.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(s.getMatch()).isSameAs(match);
        });
        // findById 가 좌석마다 호출되지 않음 (캐싱 검증)
        verify(sectionRepository, never()).findById(any());
        verify(seatGradeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("좌석_일괄등록_match_없음_MATCH_NOT_FOUND")
    void 좌석_일괄등록_회차없음() {
        // given
        given(matchRepository.findById(MATCH_ID)).willReturn(Optional.empty());
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1)
        ));

        // when & then
        assertThatThrownBy(() -> service.bulkCreate(MATCH_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("좌석_일괄등록_요청에없는_section_포함시_SECTION_NOT_FOUND")
    void 좌석_일괄등록_구역누락() {
        // given — DB 에 section 1개만, 요청엔 11L 도 포함
        given(sectionRepository.findAllById(any())).willReturn(List.of(section));
        given(seatGradeRepository.findAllById(any())).willReturn(List.of(grade));
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1),
                createReq(11L, GRADE_ID, "A", 2)
        ));

        // when & then
        assertThatThrownBy(() -> service.bulkCreate(MATCH_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SECTION_NOT_FOUND);
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("좌석_일괄등록_요청에없는_grade_포함시_SEAT_GRADE_NOT_FOUND")
    void 좌석_일괄등록_등급누락() {
        // given
        given(sectionRepository.findAllById(any())).willReturn(List.of(section));
        given(seatGradeRepository.findAllById(any())).willReturn(List.of(grade));
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1),
                createReq(SECTION_ID, 99L, "A", 2)
        ));

        // when & then
        assertThatThrownBy(() -> service.bulkCreate(MATCH_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_GRADE_NOT_FOUND);
    }

    @Test
    @DisplayName("좌석_일괄등록_페이로드내부_코드중복_DUPLICATE_SEAT_CODE")
    void 좌석_일괄등록_페이로드중복() {
        // given — (A, 1) 좌석이 두 번 → "VIP-A-1" 중복
        given(sectionRepository.findAllById(any())).willReturn(List.of(section));
        given(seatGradeRepository.findAllById(any())).willReturn(List.of(grade));
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1),
                createReq(SECTION_ID, GRADE_ID, "A", 1)
        ));

        // when & then
        assertThatThrownBy(() -> service.bulkCreate(MATCH_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SEAT_CODE);
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("좌석_일괄등록_DB와_코드충돌_DUPLICATE_SEAT_CODE")
    void 좌석_일괄등록_DB중복() {
        // given — DB 에 이미 "VIP-A-1" 존재
        given(sectionRepository.findAllById(any())).willReturn(List.of(section));
        given(seatGradeRepository.findAllById(any())).willReturn(List.of(grade));
        given(seatRepository.findExistingSectionCodePairs(eq(MATCH_ID), anyCollection()))
                .willReturn(List.<Object[]>of(new Object[]{SECTION_ID, "VIP-A-1"}));
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1)
        ));

        // when & then
        assertThatThrownBy(() -> service.bulkCreate(MATCH_ID, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SEAT_CODE);
    }

    @Test
    @DisplayName("좌석_일괄등록_다른구역_같은코드_허용_(VIP-A-1)을_두_구역에_각각_등록")
    void 좌석_일괄등록_다른구역_같은코드_허용() {
        // given — 같은 코드 "VIP-A-1" 이지만 sectionId 가 다름 → 페이로드 내 충돌 없음
        Long otherSectionId = 11L;
        Section otherSection = Section.create(event, "중앙", 2, "");
        ReflectionTestUtils.setField(otherSection, "id", otherSectionId);
        given(sectionRepository.findAllById(any())).willReturn(List.of(section, otherSection));
        given(seatGradeRepository.findAllById(any())).willReturn(List.of(grade));
        given(seatRepository.findExistingSectionCodePairs(eq(MATCH_ID), anyCollection()))
                .willReturn(List.of());
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1),
                createReq(otherSectionId, GRADE_ID, "A", 1)
        ));

        // when
        service.bulkCreate(MATCH_ID, req);

        // then — 두 좌석 모두 saveAll
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(Seat::getSeatCode)
                .containsExactly("VIP-A-1", "VIP-A-1");
    }

    @Test
    @DisplayName("좌석_일괄등록_DB에_다른구역_같은코드_있어도_통과")
    void 좌석_일괄등록_DB다른구역_같은코드_허용() {
        // given — DB 에는 11L 구역에 "VIP-A-1" 이 있고, 페이로드는 SECTION_ID(10L) + "VIP-A-1" → 충돌 X
        given(sectionRepository.findAllById(any())).willReturn(List.of(section));
        given(seatGradeRepository.findAllById(any())).willReturn(List.of(grade));
        given(seatRepository.findExistingSectionCodePairs(eq(MATCH_ID), anyCollection()))
                .willReturn(List.<Object[]>of(new Object[]{11L, "VIP-A-1"}));
        AdminSeatBulkCreateRequest req = bulkReq(List.of(
                createReq(SECTION_ID, GRADE_ID, "A", 1)
        ));

        // when
        service.bulkCreate(MATCH_ID, req);

        // then
        verify(seatRepository).saveAll(any());
    }

    // ─── 좌석 전체 조회 ─────────────────────────────────────

    @Test
    @DisplayName("좌석_전체조회_fetch_join_결과_매핑")
    void 좌석_전체조회_성공() {
        // given
        given(seatRepository.findAllWithSectionAndGradeByMatchId(MATCH_ID))
                .willReturn(List.of(availableSeat));

        // when
        List<AdminSeatResponse> res = service.findAllByMatch(MATCH_ID);

        // then
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getSectionName()).isEqualTo("좌측");
        assertThat(res.get(0).getGradeCode()).isEqualTo("VIP");
        assertThat(res.get(0).getPrice()).isEqualTo(100000);
        assertThat(res.get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("좌석_전체조회_match_없음_MATCH_NOT_FOUND")
    void 좌석_전체조회_회차없음() {
        // given
        given(matchRepository.existsById(MATCH_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> service.findAllByMatch(MATCH_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
        verify(seatRepository, never()).findAllWithSectionAndGradeByMatchId(any());
    }

    // ─── update ─────────────────────────────────────────────

    @Test
    @DisplayName("좌석_수정_section_grade_둘다_변경")
    void 좌석_수정_둘다변경() {
        // given — 좌측 → 중앙으로 이동, VIP → R 로 변경
        Section newSection = Section.create(event, "중앙", 2, "");
        ReflectionTestUtils.setField(newSection, "id", 11L);
        SeatGrade newGrade = SeatGrade.create(event, "R", 80000, "#00FF00");
        ReflectionTestUtils.setField(newGrade, "id", 21L);

        given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(availableSeat));
        given(sectionRepository.findById(11L)).willReturn(Optional.of(newSection));
        given(seatGradeRepository.findById(21L)).willReturn(Optional.of(newGrade));

        // when
        AdminSeatResponse res = service.update(SEAT_ID, updateReq(11L, 21L));

        // then — 진짜 엔티티 상태: changeSectionAndGrade() 가 둘 다 바꿨는지
        assertThat(availableSeat.getSection()).isSameAs(newSection);
        assertThat(availableSeat.getSeatGrade()).isSameAs(newGrade);
        assertThat(res.getSectionName()).isEqualTo("중앙");
        assertThat(res.getGradeCode()).isEqualTo("R");
    }

    @Test
    @DisplayName("좌석_수정_grade만_변경시_section은_그대로")
    void 좌석_수정_등급만() {
        // given
        SeatGrade newGrade = SeatGrade.create(event, "R", 80000, "#00FF00");
        ReflectionTestUtils.setField(newGrade, "id", 21L);
        given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(availableSeat));
        given(seatGradeRepository.findById(21L)).willReturn(Optional.of(newGrade));

        // when
        service.update(SEAT_ID, updateReq(null, 21L));

        // then — changeSectionAndGrade(null, grade) 의 null-safe 동작 검증
        verify(sectionRepository, never()).findById(any());
        assertThat(availableSeat.getSection()).isSameAs(section);   // 안 바뀜
        assertThat(availableSeat.getSeatGrade()).isSameAs(newGrade);    // 바뀜
    }

    @Test
    @DisplayName("좌석_수정_SOLD상태_SEAT_NOT_EDITABLE")
    void 좌석_수정_편집불가() {
        // given
        Seat soldSeat = Seat.create(match, section, grade, "A", 1, "VIP-A-1");
        ReflectionTestUtils.setField(soldSeat, "id", SEAT_ID);
        ReflectionTestUtils.setField(soldSeat, "status", SeatStatus.SOLD);
        given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(soldSeat));

        // when & then
        assertThatThrownBy(() -> service.update(SEAT_ID, updateReq(11L, 21L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_EDITABLE);
        assertThat(soldSeat.getStatus()).isEqualTo(SeatStatus.SOLD);
    }

    @Test
    @DisplayName("좌석_수정_없음_SEAT_NOT_FOUND")
    void 좌석_수정_없음() {
        // given
        given(seatRepository.findById(SEAT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.update(SEAT_ID, updateReq(11L, 21L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
    }

    // ─── delete ─────────────────────────────────────────────

    @Test
    @DisplayName("좌석_삭제_정상")
    void 좌석_삭제_성공() {
        // given
        given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(availableSeat));

        // when
        service.delete(SEAT_ID);

        // then
        verify(seatRepository).delete(availableSeat);
    }

    @Test
    @DisplayName("좌석_삭제_SOLD상태_SEAT_NOT_DELETABLE")
    void 좌석_삭제_불가() {
        // given
        Seat soldSeat = Seat.create(match, section, grade, "A", 1, "VIP-A-1");
        ReflectionTestUtils.setField(soldSeat, "id", SEAT_ID);
        ReflectionTestUtils.setField(soldSeat, "status", SeatStatus.SOLD);
        given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(soldSeat));

        // when & then
        assertThatThrownBy(() -> service.delete(SEAT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_DELETABLE);
        verify(seatRepository, never()).delete((Seat) any());
    }

    @Test
    @DisplayName("좌석_삭제_없음_SEAT_NOT_FOUND")
    void 좌석_삭제_없음() {
        // given
        given(seatRepository.findById(SEAT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.delete(SEAT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────

    private AdminSeatCreateRequest createReq(Long sectionId, Long gradeId, String row, Integer no) {
        AdminSeatCreateRequest req = mock(AdminSeatCreateRequest.class);
        given(req.getSectionId()).willReturn(sectionId);
        given(req.getSeatGradeId()).willReturn(gradeId);
        given(req.getRowLabel()).willReturn(row);
        given(req.getSeatNo()).willReturn(no);
        return req;
    }

    private AdminSeatUpdateRequest updateReq(Long sectionId, Long gradeId) {
        AdminSeatUpdateRequest req = mock(AdminSeatUpdateRequest.class);
        given(req.getSectionId()).willReturn(sectionId);
        given(req.getSeatGradeId()).willReturn(gradeId);
        return req;
    }

    private AdminSeatBulkCreateRequest bulkReq(List<AdminSeatCreateRequest> seats) {
        AdminSeatBulkCreateRequest req = mock(AdminSeatBulkCreateRequest.class);
        given(req.getSeats()).willReturn(seats);
        return req;
    }

}