package com.ticketmaster.backend.domain.seat.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 좌석 조회
 *
 *  - AVAILABLE / RESERVED / SOLD 모두 DB 컬럼으로 관리
 *  - RESERVED 상태는 점유 시 DB에 기록, 스케줄러가 만료 처리
 *
 * availableCount 계산: status == AVAILABLE 좌석만 카운트
 * (= SOLD / RESERVED 모두 제외)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final MatchRepository matchRepository;

    /**
     * 좌석 선택 1단계 - 구역 목록 + 등급별 잔여
     * 캐시: 키=matchId, TTL 2초 - 폭주 새로고침을 주기당 DB 1회로 수렴
     */
    @Cacheable(cacheNames = "seat_sections", key = "#matchId")
    public SeatSectionListResponse findSectionsByMatch(Long matchId) {
        // 1. 매치 → 이벤트 정보 확인 (없으면 404)
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        Long eventId = match.getEvent().getId();

        // 2. 이벤트의 구역 목록과 등급 목록 조회
        List<Section> sections = sectionRepository.findAllByEventIdOrderByDisplayOrderAsc(eventId);
        List<SeatGrade> grades = seatGradeRepository.findAllByEventIdOrderByPriceDesc(eventId);

        // 3. 매치 좌석 한 번에 로딩 — Object[]{seatId, sectionId, gradeId, status}
        //    Entity 전체 대신 필요 컬럼만 가져와 N+1 / 메모리 절약
        List<Object[]> rows = seatRepository.findIdAndGroupingByMatchId(matchId);

        // 4. 구역별/등급별 잔여 카운트 (AVAILABLE 좌석만 카운트)
        Map<Long, Long> sectionAvailable = new HashMap<>(); // sectionId → 잔여수
        Map<Long, Long> gradeAvailable = new HashMap<>();   // gradeId → 잔여수
        for (Object[] r : rows) {
            Long sectionId = (Long) r[1];
            Long gradeId = (Long) r[2];
            SeatStatus status = (SeatStatus) r[3];

            if (status == SeatStatus.AVAILABLE) {
                // merge: 키 없으면 1L 저장, 있으면 기존값에 +1
                sectionAvailable.merge(sectionId, 1L, Long::sum);
                gradeAvailable.merge(gradeId, 1L, Long::sum);
            }
        }

        // 5. 구역 목록 + 잔여수를 응답 DTO로 변환
        //    잔여 0석 구역은 Map에 키가 없으므로 getOrDefault(0L)로 안전 처리
        List<SeatSectionListResponse.SectionItem> sectionItems = sections.stream()
                .map(s -> SeatSectionListResponse.SectionItem.of(
                        s.getId(), s.getName(), s.getDisplayOrder(),
                        sectionAvailable.getOrDefault(s.getId(), 0L)))
                .toList();

        // 6. 등급도 동일 패턴
        List<SeatSectionListResponse.GradeAvailability> gradeItems = grades.stream()
                .map(g -> SeatSectionListResponse.GradeAvailability.of(
                        g.getGradeCode(), g.getColorHex(),
                        gradeAvailable.getOrDefault(g.getId(), 0L)))
                .toList();

        return SeatSectionListResponse.of(matchId, sectionItems, gradeItems);
    }

    /**
     * 좌석 선택 2단계 — 구역 내 좌석
     * 캐시: 키=matchId + ':' + sectionId (두 파라미터 조합), TTL 2초
     */
    @Cacheable(cacheNames = "seat_section_details", key = "#matchId + ':' + #sectionId")
    public SectionSeatListResponse findSeatsBySection(Long matchId, Long sectionId) {
        // 1. 매치/구역 존재 검증
        if (!matchRepository.existsById(matchId)) {
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
        }
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));

        // 2. 해당 구역의 좌석 전체 조회
        List<Seat> seats = seatRepository.findBySectionAndMatch(matchId, sectionId);

        // 3. DB Seat.status를 그대로 응답에 노출
        List<SectionSeatListResponse.SeatItem> items = seats.stream()
                .map(SectionSeatListResponse.SeatItem::from)
                .toList();

        return SectionSeatListResponse.of(matchId, sectionId, section.getName(), items);
    }
}
