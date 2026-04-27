package com.ticketmaster.backend.admin.seatgrade.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** PATCH 부분 수정 — 변경할 필드만 전송, 나머지는 null */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGradeUpdateRequest {

    @Positive(message = "가격은 0보다 커야 합니다.")
    private Integer price;

    @Pattern(
            regexp = "^#[A-Fa-f0-9]{6}$",
            message = "색상은 #RRGGBB 형식의 HEX 코드여야 합니다."
    )
    private String colorHex;
}
