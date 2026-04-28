package com.ticketmaster.backend.admin.section.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SectionCreateRequest {

    @NotBlank(message = "구역명은 필수입니다.")
    private String name;

    @NotNull(message = "표시 순서는 필수입니다.")
    @Positive(message = "표시 순서는 양수여야 합니다.")
    private Integer displayOrder;

    private String description; // 선택 — null/공백 허용
}
