package com.ticketmaster.backend.domain.seat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 일괄 점유 요청
 * - maxTicketsPerUser 상한 검증은 Service 에서
 */
@Schema(description = "좌석 점유 요청")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SeatReserveRequest {

    @Schema(description = "점유할 좌석 ID 목록", example = "[101, 102, 103]")
    @NotEmpty(message = "좌석을 선택해주세요.")
    private List<Long> seatIds;
}
