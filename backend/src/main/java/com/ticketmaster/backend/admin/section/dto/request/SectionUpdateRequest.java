package com.ticketmaster.backend.admin.section.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SectionUpdateRequest {

    /**
     * - null 이면 "이름은 변경하지 않음" (PATCH 시맨틱)
     * - 값이 있으면 공백이 아닌 문자가 최소 1개는 있어야 함 ("" / "   " 차단)
     */
    @Pattern(regexp = ".*\\S.*", message = "구역명을 입력해주세요.")
    private String name;

    @Positive(message = "표시 순서는 양수여야 합니다.")
    private Integer displayOrder;

    private String description;
}
