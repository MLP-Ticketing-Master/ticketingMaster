package com.ticketmaster.backend.domain.seat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 해제 요청
 */
@Schema(description = "좌석 해제 요청")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SeatReleaseRequest {

    @Schema(description = "해제할 좌석 ID 목록", example = "[101, 102]")
    @NotEmpty(message = "해제할 좌석이 없습니다.")
    private List<Long> seatIds;
}
