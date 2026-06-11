package com.ticketmaster.backend.domain.seat.service;


import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.dto.response.SeatSectionListResponse;
import com.ticketmaster.backend.domain.seat.dto.response.SectionSeatListResponse;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock private SeatRepository seatRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SeatGradeRepository seatGradeRepository;
    @Mock private MatchRepository matchRepository;

    @InjectMocks
    private SeatService seatService;

    private Event event;
    private Match match;
    private Section leftSection;
    private Section centerSection;
    private Section rightSection;
    private SeatGrade vipGrade;
    private SeatGrade rGrade;

    @BeforeEach
    void setUp() {
        event = createEvent(1L);
        match = createMatch(10L, event);

        // 구역 3개 — 좌측/중앙/우측
        leftSection   = createSection(101L, event, "좌측", 1);
        centerSection = createSection(102L, event, "중앙", 2);
        rightSection  = createSection(103L, event, "우측", 3);

        // 등급 2개 — VIP / R
        vipGrade = createSeatGrade(201L, event, "VIP", 100000, "#FFD700");
        rGrade   = createSeatGrade(202L, event, "R",    70000, "#C0C0C0");
    }

    @Nested
    @DisplayName("1단계 구역 목록 조회 — findSectionsByMatch")
    class FindSectionsByMatch {

        @Test
        @DisplayName("TC-01~05: 정상 조회 — AVAILABLE만 카운트, RESERVED/SOLD 제외")
        void 정상_조회_AVAILABLE_좌석만_카운트() {
            // given
            given(matchRepository.findById(10L)).willReturn(Optional.of(match));
            given(sectionRepository.findAllByEventIdOrderByDisplayOrderAsc(1L))
                    .willReturn(List.of(leftSection, centerSection, rightSection));
            given(seatGradeRepository.findAllByEventIdOrderByPriceDesc(1L))
                    .willReturn(List.of(vipGrade, rGrade));

            // 좌석 시드:
            //   좌측(101) VIP(201): AVAILABLE 2, RESERVED 1, SOLD 1 → 잔여 2
            //   중앙(102) R(202):   AVAILABLE 3 → 잔여 3
            //   우측(103):          좌석 0개 → 잔여 0
            given(seatRepository.findIdAndGroupingByMatchId(10L)).willReturn(List.of(
                    row(1L, 101L, 201L, SeatStatus.AVAILABLE),
                    row(2L, 101L, 201L, SeatStatus.AVAILABLE),
                    row(3L, 101L, 201L, SeatStatus.RESERVED),  // 카운트 제외
                    row(4L, 101L, 201L, SeatStatus.SOLD),       // 카운트 제외
                    row(5L, 102L, 202L, SeatStatus.AVAILABLE),
                    row(6L, 102L, 202L, SeatStatus.AVAILABLE),
                    row(7L, 102L, 202L, SeatStatus.AVAILABLE)
            ));

            // when
            SeatSectionListResponse response = seatService.findSectionsByMatch(10L);

            // then
            assertThat(response.getMatchId()).isEqualTo(10L);
            assertThat(response.getSections()).hasSize(3);

            // TC-01,02,03,04: 구역별 잔여 (displayOrder 정렬, AVAILABLE만 카운트)
            assertThat(response.getSections())
                    .extracting("sectionId", "name", "displayOrder", "availableCount")
                    .containsExactly(
                            tuple(101L, "좌측", 1, 2L),
                            tuple(102L, "중앙", 2, 3L),
                            tuple(103L, "우측", 3, 0L)  // 좌석 없는 구역도 0L로 안전 처리
                    );

            // TC-05: 등급별 잔여 (가격 내림차순)
            assertThat(response.getGradeAvailability())
                    .extracting("gradeCode", "colorHex", "availableCount")
                    .containsExactly(
                            tuple("VIP", "#FFD700", 2L),
                            tuple("R",   "#C0C0C0", 3L)
                    );
        }

        @Test
        @DisplayName("TC-06: 존재하지 않는 matchId → MATCH_NOT_FOUND")
        void 매치없음() {
            // given
            given(matchRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> seatService.findSectionsByMatch(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("2단계 구역 좌석 조회 — findSeatsBySection")
    class FindSeatsBySection {

        @Test
        @DisplayName("TC-07~10: 정상 조회 — DB status 그대로 반환, 등급/색상/가격 포함")
        void 정상_조회_DB상태_그대로_반환() {
            // given
            given(matchRepository.existsById(10L)).willReturn(true);
            given(sectionRepository.findById(101L)).willReturn(Optional.of(leftSection));

            // 좌석 3개 — 상태별 각각 1개씩 (AVAILABLE/RESERVED/SOLD)
            Seat s1 = createSeat(1L, match, leftSection, vipGrade, "A", 1, "VIP-A-1", SeatStatus.AVAILABLE);
            Seat s2 = createSeat(2L, match, leftSection, vipGrade, "A", 2, "VIP-A-2", SeatStatus.RESERVED);
            Seat s3 = createSeat(3L, match, leftSection, vipGrade, "A", 3, "VIP-A-3", SeatStatus.SOLD);
            given(seatRepository.findBySectionAndMatch(10L, 101L))
                    .willReturn(List.of(s1, s2, s3));

            // when
            SectionSeatListResponse response = seatService.findSeatsBySection(10L, 101L);

            // then
            assertThat(response.getMatchId()).isEqualTo(10L);
            assertThat(response.getSectionId()).isEqualTo(101L);
            assertThat(response.getSectionName()).isEqualTo("좌측");
            assertThat(response.getSeats()).hasSize(3);

            // TC-07,08: DB status가 응답에 그대로 노출
            // TC-09: 등급 코드/색상/가격 포함
            // TC-10: 정렬은 Repository ORDER BY가 처리 (여기선 시드 순서대로)
            assertThat(response.getSeats())
                    .extracting("seatId", "seatCode", "status", "gradeCode", "colorHex", "price")
                    .containsExactly(
                            tuple(1L, "VIP-A-1", SeatStatus.AVAILABLE, "VIP", "#FFD700", 100000),
                            tuple(2L, "VIP-A-2", SeatStatus.RESERVED,  "VIP", "#FFD700", 100000),
                            tuple(3L, "VIP-A-3", SeatStatus.SOLD,      "VIP", "#FFD700", 100000)
                    );
        }

        @Test
        @DisplayName("TC-06: 존재하지 않는 matchId → MATCH_NOT_FOUND")
        void 매치없음() {
            // given
            given(matchRepository.existsById(999L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> seatService.findSeatsBySection(999L, 101L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCH_NOT_FOUND);
        }

        @Test
        @DisplayName("TC-11: 존재하지 않는 sectionId → SECTION_NOT_FOUND")
        void 구역없음() {
            // given
            given(matchRepository.existsById(10L)).willReturn(true);
            given(sectionRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> seatService.findSeatsBySection(10L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SECTION_NOT_FOUND);
        }
    }

    // 시드 헬퍼 — id 같은 setter 없는 필드는 ReflectionTestUtils로 주입

    private Event createEvent(Long id) {
        Event e = Event.builder().build();
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private Match createMatch(Long id, Event event) {
        Match m = Match.builder()
                .event(event)
                .roundLabel("1R")
                .matchDate(LocalDate.now())
                .startAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private Section createSection(Long id, Event event, String name, int displayOrder) {
        Section s = Section.create(event, name, displayOrder, null);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    private SeatGrade createSeatGrade(Long id, Event event, String code, int price, String colorHex) {
        SeatGrade g = SeatGrade.create(event, code, price, colorHex);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    private Seat createSeat(Long id, Match match, Section section, SeatGrade grade,
                            String rowLabel, int seatNo, String seatCode, SeatStatus status) {
        Seat s = Seat.create(match, section, grade, rowLabel, seatNo, seatCode);
        ReflectionTestUtils.setField(s, "id", id);
        ReflectionTestUtils.setField(s, "status", status);
        return s;
    }

    /** findIdAndGroupingByMatchId가 반환하는 Object[] 시뮬레이션 */
    private Object[] row(Long seatId, Long sectionId, Long gradeId, SeatStatus status) {
        return new Object[]{seatId, sectionId, gradeId, status};
    }
}