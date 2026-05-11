package com.ticketmaster.backend.domain.seat.dto.response;

import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 선택 2단계 응답 — 특정 구역의 전체 좌석
 *
 * GET /matches/{matchId}/sections/{sectionId}/seats
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SectionSeatListResponse {

    private Long matchId;          // 경기 ID
    private Long sectionId;        // 구역 ID
    private String sectionName;    // 구역명 (예: "중앙")
    private List<SeatItem> seats;  // 해당 구역의 전체 좌석 목록

    public static SectionSeatListResponse of(Long matchId, Long sectionId,
                                             String sectionName, List<SeatItem> seats) {
        return new SectionSeatListResponse(matchId, sectionId, sectionName, seats);
    }

    /** 좌석 단위 정보 — 좌석 그리드 렌더링에 필요한 모든 정보 */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SeatItem {
        private Long seatId;           // 좌석 ID
        private String seatCode;       // 좌석 코드 (예: "VIP-A-15")
        private String rowLabel;       // 행 라벨 (예: "A", "B")
        private int seatNo;            // 행 내 좌석 번호 (예: 15)
        private String gradeCode;      // 등급 코드 (VIP, R, S, A)
        private String colorHex;       // 등급 색상 (예: #FFD700)
        private int price;             // 좌석 가격
        private SeatStatus status;     // DB Seat.status 값 그대로 (AVAILABLE/RESERVED/SOLD)

        /** Seat 엔티티 → SeatItem 변환 */
        public static SeatItem from(Seat s) {
            return new SeatItem(
                    s.getId(),
                    s.getSeatCode(),
                    s.getRowLabel(),
                    s.getSeatNo(),
                    s.getSeatGrade().getGradeCode(),
                    s.getSeatGrade().getColorHex(),
                    s.getSeatGrade().getPrice(),
                    s.getStatus()
            );
        }
    }
}
