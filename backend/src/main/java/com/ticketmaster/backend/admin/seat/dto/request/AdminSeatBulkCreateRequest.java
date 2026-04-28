package com.ticketmaster.backend.admin.seat.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 그리드 초기 세팅용 (행×열 일괄 등록)
 * - 상한선: 한 번에 최대 5000석 (트랜잭션 길어지면 락/메모리 부담)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSeatBulkCreateRequest {

    @NotEmpty(message = "등록할 좌석을 1개 이상 입력해주세요.")
    @Size(max = 5000, message = "한 번에 최대 5,000개까지 등록할 수 있습니다.")
    @Valid  // 리스트 안의 SeatCreateRequest 각각도 검증
    private List<AdminSeatCreateRequest> seats;
}
