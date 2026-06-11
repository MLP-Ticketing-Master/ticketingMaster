package com.ticketmaster.backend.domain.seat.dto.response;

import com.ticketmaster.backend.domain.seat.entity.SeatGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SeatGradeResponse {
    private Long seatGradeId;
    private String gradeCode;
    private int price;
    private String colorHex;

    // Entity -> DTO
    public static SeatGradeResponse from(SeatGrade seatGrade) {
        return SeatGradeResponse.builder()
                .seatGradeId(seatGrade.getId())
                .gradeCode(seatGrade.getGradeCode())
                .price(seatGrade.getPrice())
                .colorHex(seatGrade.getColorHex())
                .build();
    }
}
