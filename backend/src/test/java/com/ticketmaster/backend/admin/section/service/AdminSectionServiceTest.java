package com.ticketmaster.backend.admin.section.service;

import com.ticketmaster.backend.admin.section.dto.request.AdminSectionCreateRequest;
import com.ticketmaster.backend.admin.section.dto.request.AdminSectionUpdateRequest;
import com.ticketmaster.backend.admin.section.dto.response.AdminSectionResponse;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.seat.entity.Section;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminSectionServiceTest {

    @InjectMocks
    private AdminSectionService service;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private EventRepository eventRepository;

    private static final Long EVENT_ID = 1L;
    private static final Long SECTION_ID = 100L;

    private Event event;
    private Section leftSection;   // name="좌측",  displayOrder=1
    private Section centerSection;  // name="중앙",  displayOrder=2

    @BeforeEach
    void setUp() {
        event = mock(Event.class);
        given(event.getId()).willReturn(EVENT_ID);

        leftSection = Section.create(event, "좌측", 1, "무대 좌측 스탠드");
        ReflectionTestUtils.setField(leftSection, "id", SECTION_ID);

        centerSection = Section.create(event, "중앙", 2, "무대 정면 스탠드");
        ReflectionTestUtils.setField(centerSection, "id", 101L);
    }

    // ─── 전체 조회 ───────────────────────────────────

    @Test
    @DisplayName("구역_목록을_displayOrder_오름차순으로_반환")
    void 구역_목록조회_성공() {
        // given
        given(eventRepository.existsById(EVENT_ID)).willReturn(true);
        given(sectionRepository.findAllByEventIdOrderByDisplayOrderAsc(EVENT_ID))
                .willReturn(List.of(leftSection, centerSection));

        // when
        List<AdminSectionResponse> result = service.findAllByEvent(EVENT_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("좌측");
        assertThat(result.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(result.get(1).getName()).isEqualTo("중앙");
        assertThat(result.get(1).getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("구역_목록조회_대회없음_EVENT_NOT_FOUND")
    void 구역_목록조회_대회없음() {
        // given
        given(eventRepository.existsById(EVENT_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> service.findAllByEvent(EVENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVENT_NOT_FOUND);
    }

    // ─── create ─────────────────────────────────────────────

    @Test
    @DisplayName("신규_구역_등록_성공")
    void 구역_등록_신규() {
        // given — "우측" 신규 등록
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
        given(sectionRepository.existsByEventIdAndName(EVENT_ID, "우측")).willReturn(false);
        given(sectionRepository.existsByEventIdAndDisplayOrder(EVENT_ID, 3)).willReturn(false);
        given(sectionRepository.save(any(Section.class)))
                .willAnswer(inv -> {
                    Section s = inv.getArgument(0);
                    ReflectionTestUtils.setField(s, "id", 200L);
                    return s;
                });

        // when
        AdminSectionResponse res = service.create(EVENT_ID, createReq("우측", 3, "무대 우측 스탠드"));

        // then
        assertThat(res.getSectionId()).isEqualTo(200L);
        assertThat(res.getName()).isEqualTo("우측");
        assertThat(res.getDisplayOrder()).isEqualTo(3);

        ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
        verify(sectionRepository).save(captor.capture());
        assertThat(captor.getValue().getEvent()).isSameAs(event);
    }

    @Test
    @DisplayName("구역_등록_이름_양옆공백_제거후_저장")
    void 구역_등록_이름_트림() {
        // given — 새 구역 "우측" 등록 (order=3)
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
        given(sectionRepository.existsByEventIdAndName(EVENT_ID, "우측")).willReturn(false);
        given(sectionRepository.existsByEventIdAndDisplayOrder(EVENT_ID, 3)).willReturn(false);
        given(sectionRepository.save(any(Section.class)))
                .willAnswer(inv -> {
                    Section s = inv.getArgument(0);
                    ReflectionTestUtils.setField(s, "id", 200L);
                    return s;
                });

        // when
        AdminSectionResponse res = service.create(EVENT_ID, createReq("  우측  ", 3, "  무대 우측 스탠드  "));

        // then — 트림된 이름으로 중복 체크 + save 시 진짜 Section 의 필드값
        verify(sectionRepository).existsByEventIdAndName(EVENT_ID, "우측");
        ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
        verify(sectionRepository).save(captor.capture());
        Section saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("우측");
        assertThat(saved.getDescription()).isEqualTo("무대 우측 스탠드");
        assertThat(res.getName()).isEqualTo("우측");
    }

    @Test
    @DisplayName("구역_등록_이름_중복시_DUPLICATE_SECTION_NAME")
    void 구역_등록_이름중복() {
        // given — "좌측" 재등록 시도 (이미 setUp 에 있음)
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
        given(sectionRepository.existsByEventIdAndName(EVENT_ID, "좌측")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.create(EVENT_ID, createReq("좌측", 3, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SECTION_NAME);
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("구역_등록_displayOrder_중복시_DUPLICATE_SECTION_DISPLAY_ORDER")
    void 구역_등록_순서중복() {
        // given - "우측" 등록 시도, displayOrder=1 (이미 좌측이 차지)
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
        given(sectionRepository.existsByEventIdAndName(EVENT_ID, "우측")).willReturn(false);
        given(sectionRepository.existsByEventIdAndDisplayOrder(EVENT_ID, 1)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.create(EVENT_ID, createReq("우측", 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SECTION_DISPLAY_ORDER);
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("구역_등록_대회없음_EVENT_NOT_FOUND")
    void 구역_등록_대회없음() {
        // given
        given(eventRepository.findById(EVENT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.create(EVENT_ID, createReq("우측", 3, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EVENT_NOT_FOUND);
        verify(sectionRepository, never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────

    @Test
    @DisplayName("구역_수정_정상")
    void 구역_수정_성공() {
        // given — leftSection 의 이름·순서·설명 모두 변경
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(leftSection));
        given(sectionRepository.existsByEventIdAndName(EVENT_ID, "우측")).willReturn(false);
        given(sectionRepository.existsByEventIdAndDisplayOrder(EVENT_ID, 3)).willReturn(false);

        // when
        AdminSectionResponse res = service.update(SECTION_ID, updateReq("우측", 3, "무대 우측 스탠드"));

        // then — 진짜 엔티티 상태 검증
        assertThat(leftSection.getName()).isEqualTo("우측");
        assertThat(leftSection.getDisplayOrder()).isEqualTo(3);
        assertThat(leftSection.getDescription()).isEqualTo("무대 우측 스탠드");
        assertThat(res.getName()).isEqualTo("우측");
        assertThat(res.getDisplayOrder()).isEqualTo(3);
        assertThat(res.getDescription()).isEqualTo("무대 우측 스탠드");
    }

    @Test
    @DisplayName("구역_수정_이름_동일하면_중복체크_생략")
    void 구역_수정_이름동일() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(leftSection));

        // when
        service.update(SECTION_ID, updateReq("좌측", null, null));

        // then
        verify(sectionRepository, never()).existsByEventIdAndName(any(), any());
        assertThat(leftSection.getName()).isEqualTo("좌측");
        assertThat(leftSection.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("구역_수정_이름변경_충돌시_DUPLICATE_SECTION_NAME")
    void 구역_수정_이름충돌() {
        // given - leftSection 을 "중앙" 으로 바꾸려 함
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(leftSection));
        given(sectionRepository.existsByEventIdAndName(EVENT_ID, "중앙")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.update(SECTION_ID, updateReq("중앙", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SECTION_NAME);
        assertThat(leftSection.getName()).isEqualTo("좌측");
    }

    @Test
    @DisplayName("구역_수정_displayOrder_변경_충돌시_DUPLICATE_SECTION_DISPLAY_ORDER")
    void 구역_수정_순서충돌() {
        // given — leftSection 의 order 를 2 로 바꾸려 함 (centerSection 차지)
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(leftSection));
        given(sectionRepository.existsByEventIdAndDisplayOrder(EVENT_ID, 2)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.update(SECTION_ID, updateReq(null, 2, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SECTION_DISPLAY_ORDER);
        assertThat(leftSection.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("구역_수정_부분수정_name_null_이면_기존값_유지")
    void 구역_수정_부분수정() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(leftSection));
        given(sectionRepository.existsByEventIdAndDisplayOrder(EVENT_ID, 5)).willReturn(false);

        // when
        service.update(SECTION_ID, updateReq(null, 5, "변경"));

        // then — ★ Section.update 의 null-safe 동작
        assertThat(leftSection.getName()).isEqualTo("좌측");
        assertThat(leftSection.getDisplayOrder()).isEqualTo(5);
        assertThat(leftSection.getDescription()).isEqualTo("변경");
        verify(sectionRepository, never()).existsByEventIdAndName(any(), any());
    }

    @Test
    @DisplayName("구역_수정_없음_SECTION_NOT_FOUND")
    void 구역_수정_없음() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.update(SECTION_ID, updateReq("우측", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SECTION_NOT_FOUND);
    }

    // ─── delete ──────────────────────────────────────

    @Test
    @DisplayName("구역_삭제_좌석미배정시_정상")
    void 구역_삭제_성공() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(leftSection));
        given(seatRepository.existsBySectionId(SECTION_ID)).willReturn(false);

        // when
        service.delete(SECTION_ID);

        // then
        verify(sectionRepository).delete(leftSection);
    }

    @Test
    @DisplayName("구역_삭제_사용중_SECTION_IN_USE")
    void 구역_삭제_사용중() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(leftSection));
        given(seatRepository.existsBySectionId(SECTION_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.delete(SECTION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SECTION_IN_USE);
        verify(sectionRepository, never()).delete((Section) any());
    }

    @Test
    @DisplayName("구역_삭제_없음_SECTION_NOT_FOUND")
    void 구역_삭제_없음() {
        // given
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.delete(SECTION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SECTION_NOT_FOUND);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────

    private AdminSectionCreateRequest createReq(String name, Integer order, String desc) {

        AdminSectionCreateRequest req = mock(AdminSectionCreateRequest.class);
        given(req.getName()).willReturn(name);
        given(req.getDisplayOrder()).willReturn(order);
        given(req.getDescription()).willReturn(desc);
        return req;
    }

    private AdminSectionUpdateRequest updateReq(String name, Integer order, String desc) {
        AdminSectionUpdateRequest req = mock(AdminSectionUpdateRequest.class);
        given(req.getName()).willReturn(name);
        given(req.getDisplayOrder()).willReturn(order);
        given(req.getDescription()).willReturn(desc);
        return req;
    }

}