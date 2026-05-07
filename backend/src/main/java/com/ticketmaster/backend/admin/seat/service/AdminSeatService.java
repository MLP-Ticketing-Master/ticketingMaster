package com.ticketmaster.backend.admin.seat.service;

import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatBulkCreateRequest;
import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatCreateRequest;
import com.ticketmaster.backend.admin.seat.dto.request.AdminSeatUpdateRequest;
import com.ticketmaster.backend.admin.seat.dto.response.AdminSeatResponse;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import com.ticketmaster.backend.domain.seat.entity.Section;
import com.ticketmaster.backend.domain.seat.repository.SeatGradeRepository;
import com.ticketmaster.backend.domain.seat.repository.SeatRepository;
import com.ticketmaster.backend.domain.seat.repository.SectionRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSeatService {

    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final MatchRepository matchRepository;

    // 좌석 단건 등록
    @Transactional
    public AdminSeatResponse create(Long matchId, AdminSeatCreateRequest req) {
        // 1) 회차/구역/등급 존재 검증
        Match match = findMatch(matchId);
        Section section = findSection(req.getSectionId());
        SeatGrade grade = findGrade(req.getSeatGradeId());

        // 2) seatCode 자동 조합 — {gradeCode}-{rowLabel}-{seatNo}  ex) "VIP-A-1"
        String seatCode = buildSeatCode(grade, req.getRowLabel(), req.getSeatNo());

        // 3) 회차 내 seatCode 중복 차단
        if (seatRepository.existsByMatchIdAndSeatCode(matchId, seatCode)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE);
        }

        // 4) 정적 팩토리로 생성 — 등록 시 status 는 항상 AVAILABLE
        Seat saved = seatRepository.save(
                Seat.create(match, section, grade, req.getRowLabel(), req.getSeatNo(), seatCode)
        );
        return AdminSeatResponse.from(saved);
    }

    // 좌석 일괄 등록 (100~5000개 초기 세팅)
    @Transactional(timeout = 30) // 30초 안에 끝내라, 아니면 취소한다
    public List<AdminSeatResponse> bulkCreate(Long matchId, AdminSeatBulkCreateRequest req) {
        Match match = findMatch(matchId);

        // 1) section/grade 캐싱 — 여러 좌석이 같은 section/grade 를 공유하므로(반복 조회),
        //    등장하는 ID 들을 한 번에 모아 미리 로딩해두고 메모리에서 꺼내쓰기
        //    ※ seatCode 자동 조합에 grade 가 필요하므로 중복 검증보다 먼저 로딩
        Set<Long> sectionIds = req.getSeats().stream()
                .map(AdminSeatCreateRequest::getSectionId).collect(Collectors.toSet());
        Set<Long> gradeIds = req.getSeats().stream()
                .map(AdminSeatCreateRequest::getSeatGradeId).collect(Collectors.toSet());

        // 좌석들이 쓰는 section/grade 를 한 번에 IN 절로 가져와 Map 캐싱 (N+1 방지)
        Map<Long, Section> sectionMap = sectionRepository.findAllById(sectionIds).stream()
                .collect(Collectors.toMap(Section::getId, s -> s));
        Map<Long, SeatGrade> gradeMap = seatGradeRepository.findAllById(gradeIds).stream()
                .collect(Collectors.toMap(SeatGrade::getId, g -> g));

        // 요청에 담긴 ID 중 DB 에 없는 게 있으면 차단 (예: sectionId 999 가 들어왔는데 실제로 그런 구역 없음)
        if (sectionMap.size() != sectionIds.size()) {
            throw new BusinessException(ErrorCode.SECTION_NOT_FOUND);
        }
        if (gradeMap.size() != gradeIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_GRADE_NOT_FOUND);
        }

        // 2) seatCode 자동 조합 + 페이로드 내부 중복 검증 (같은 요청 안에 같은 코드 둘 이상)
        //    ex) 관리자가 실수로 sectionId 1, gradeId 1, A, 1 을 두 번 넣었다면 → "VIP-A-1" 중복
        Map<AdminSeatCreateRequest, String> codeMap = new LinkedHashMap<>();
        Set<String> codes = new HashSet<>();
        for (AdminSeatCreateRequest s : req.getSeats()) {
            SeatGrade grade = gradeMap.get(s.getSeatGradeId());
            String code = buildSeatCode(grade, s.getRowLabel(), s.getSeatNo());
            if (!codes.add(code)) { // 코드 중복 검사
                throw new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE);
            }
            codeMap.put(s, code);
        }

        // 3) DB 와의 중복 — IN 절 한 번 쿼리로 모든 코드 검사 (N+1 방지)
        List<String> existing = seatRepository.findExistingSeatCodes(matchId, codes);
        if (!existing.isEmpty()) {
            throw new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE);
        }

        // 4) 일괄 변환 + saveAll
        List<Seat> entities = codeMap.entrySet().stream()
                .map(e -> {
                    AdminSeatCreateRequest s = e.getKey();
                    String code = e.getValue();
                    return Seat.create(
                            match,
                            sectionMap.get(s.getSectionId()),
                            gradeMap.get(s.getSeatGradeId()),
                            s.getRowLabel(), s.getSeatNo(), code);
                })
                .toList();

        seatRepository.saveAll(entities);
        return entities.stream()
                .map(AdminSeatResponse::from)
                .toList();

    }

    /**
     * 회차 좌석 전체 조회
     * - fetch join 으로 section/seatGrade 까지 한 번에 로딩 → N+1 차단
     */
    public List<AdminSeatResponse> findAllByMatch(Long matchId) {
        if (!matchRepository.existsById(matchId))
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
        return seatRepository.findAllWithSectionAndGradeByMatchId(matchId)
                .stream()
                .map(AdminSeatResponse::from)
                .toList();
    }

    /** 좌석 수정 — RESERVED/SOLD 차단, 응답은 SeatResponse 재사용 (전체 상태) */
    @Transactional
    public AdminSeatResponse update(Long seatId, AdminSeatUpdateRequest req) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
        if (!seat.isEditable()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_EDITABLE);
        }

        Section newSection = req.getSectionId() != null ? findSection(req.getSectionId()) : null;
        SeatGrade newGrade = req.getSeatGradeId() != null ? findGrade(req.getSeatGradeId()) : null;

        seat.changeSectionAndGrade(newSection, newGrade);
        return AdminSeatResponse.from(seat);
    }

    /**
     * 좌석 삭제 — RESERVED/SOLD 차단
     */
    @Transactional
    public void delete(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
        if (!seat.isDeletable()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_DELETABLE);
        }
        seatRepository.delete(seat);
    }
    // 헬퍼 메서드
    /** 좌석 코드 조합 규칙 — 단건/일괄 등록 공용 */
    private String buildSeatCode(SeatGrade grade, String rowLabel, int seatNo) {
        return grade.getGradeCode() + "-" + rowLabel + "-" + seatNo;
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));
    }

    private SeatGrade findGrade(Long id) {
        return seatGradeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_GRADE_NOT_FOUND));
    }
}
