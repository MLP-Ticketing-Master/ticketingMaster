package com.ticketmaster.backend.domain.seat.dto.response;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 선택 1단계 응답 — 구역 목록 + 등급별 잔여
 * <p>
 * GET /matches/{matchId}/sections
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SeatSectionListResponse {

    private Long matchId;                              // 경기 ID
    private List<SectionItem> sections;                // 구역 목록 (구역별 잔여 좌석 수 포함)
    private List<GradeAvailability> gradeAvailability; // 등급별 잔여 좌석 수 (VIP/R/S/A)

    public static SeatSectionListResponse of(Long matchId,
                                             List<SectionItem> sections,
                                             List<GradeAvailability> grades) {
        return new SeatSectionListResponse(matchId, sections, grades);
    }

    /** 구역 단위 정보 — 사용자가 구역 선택 시 표시 */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SectionItem {
        private Long sectionId;       // 구역 ID
        private String name;          // 구역명 (예: "좌측")
        private int displayOrder;     // 화면 정렬 순서
        private long availableCount;  // 해당 구역 잔여 좌석 수

        public static SectionItem of(Long sectionId, String name, int order, long count) {
            return new SectionItem(sectionId, name, order, count);
        }
    }

    /** 등급 단위 잔여 — 화면 상단 등급 요약/필터용 */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GradeAvailability {
        private String gradeCode;     // 등급 코드 (VIP, R, S, A)
        private String colorHex;      // 등급 색상 (#FFD700 등)
        private long availableCount;  // 해당 등급 잔여 좌석 수

        public static GradeAvailability of(String code, String hex, long count) {
            return new GradeAvailability(code, hex, count);
        }
    }
}
