package com.ticketmaster.backend.admin.seat.dto.response;

import com.ticketmaster.backend.domain.seat.entity.Seat;
import com.ticketmaster.backend.domain.seat.entity.SeatStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌석 응답 — 와이어프레임 그리드/현황표용 통합 응답
 * - POST(단건), GET(전체), PATCH 모두 이 DTO 재사용
 *   API 명세는 엔드포인트별로 일부 필드만 예시로 보여주지만,
 *   프론트가 변경 후 전체 상태를 받는 게 편하므로 동일 DTO 로 통일
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminSeatResponse {

    private Long seatId;
    private String seatCode;
    private String sectionName;
    private String gradeCode;
    private Integer price;
    private SeatStatus status;

    public static AdminSeatResponse from(Seat s) {
        return new AdminSeatResponse(
                s.getId(),
                s.getSeatCode(),
                s.getSection().getName(),
                s.getSeatGrade().getGradeCode(),
                s.getSeatGrade().getPrice(),
                s.getStatus()
        );
    }
}
