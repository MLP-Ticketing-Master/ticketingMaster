package com.ticketmaster.backend.admin.seat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌석 생성 요청 DTO
 * - 단건 등록의 요청 본문
 * - 일괄 등록의 seats 배열 요소
 * → 두 API 가 같은 입력 구조를 쓰므로 하나의 DTO 로 공유
 * - seatCode 는 받지 않음. 서버가 {gradeCode}-{rowLabel}-{seatNo} 로 자동 조합
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSeatCreateRequest {

    @NotNull(message = "구역을 선택해주세요.")
    private Long sectionId;

    @NotNull(message = "좌석 등급을 선택해주세요.")
    private Long seatGradeId;

    @NotBlank(message = "행 라벨을 입력해주세요.")
    private String rowLabel;          // "A", "B"

    @NotNull(message = "좌석 번호를 입력해주세요.")
    @Positive(message = "좌석 번호는 1 이상이어야 합니다.")
    private Integer seatNo;           // 1, 2, 3
}
