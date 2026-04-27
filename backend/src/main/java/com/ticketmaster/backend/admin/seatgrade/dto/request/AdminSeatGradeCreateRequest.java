package com.ticketmaster.backend.admin.seatgrade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /admin/events/{eventId}/seat-grades 요청 바디
 * - 등록 시점에 모두 필수 (와이어프레임 가격표가 빈 값이면 안 됨)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGradeCreateRequest {

    @NotBlank(message = "등급 코드를 입력해주세요.")
    private String gradeCode;   // "VIP", "R", "S", "A"

    @NotNull(message = "가격을 입력해주세요.")
    @Positive(message = "가격은 0보다 커야 합니다.") // 값이 양수인지 검사
    private Integer price;      // 원 단위

    @Pattern(
            regexp = "^#[A-Fa-f0-9]{6}$",
            message = "색상은 #RRGGBB 형식의 HEX 코드여야 합니다."
    )
    private String colorHex;    // "#A855F7"
}
