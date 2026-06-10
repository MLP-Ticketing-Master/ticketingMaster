package com.ticketmaster.backend.domain.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "예매 생성 요청")
@Getter
@NoArgsConstructor
public class BookingCreateRequest {

    @Schema(description = "회차 ID", example = "1")
    @NotNull(message = "matchId: 회차 ID는 필수입니다.")
    private Long matchId;

    @Schema(description = "예매할 좌석 ID 목록", example = "[101, 102]")
    @NotEmpty(message = "seatIds: 좌석은 1개 이상 선택해야 합니다.")
    private List<Long> seatIds;
}