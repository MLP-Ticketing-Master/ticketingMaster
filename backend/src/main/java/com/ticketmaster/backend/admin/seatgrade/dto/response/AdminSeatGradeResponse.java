package com.ticketmaster.backend.admin.seatgrade.dto.response;

import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import lombok.*;

/** 등록/수정 응답 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminSeatGradeResponse {

    private Long seatGradeId;
    private String gradeCode;
    private Integer price;
    private String colorHex;

    /**
     * SeatGrade 엔티티 → 응답 DTO 변환
     */
    public static AdminSeatGradeResponse from(SeatGrade entity) {
        return new AdminSeatGradeResponse(
                entity.getId(),
                entity.getGradeCode(),
                entity.getPrice(),
                entity.getColorHex()
        );
    }
}
