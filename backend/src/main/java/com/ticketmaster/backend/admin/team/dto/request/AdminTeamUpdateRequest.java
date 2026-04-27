package com.ticketmaster.backend.admin.team.dto.request;

import com.ticketmaster.backend.domain.event.entity.SportType;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminTeamUpdateRequest {

    /** 변경할 팀명 (선택) */
    @Size(max = 100, message = "팀명은 100자 이하여야 합니다.")
    private String name;

    /** 변경할 종목 (선택) */
    private SportType sportType;

    /** 변경할 로고 이미지 URL (선택) */
    @Size(max = 500, message = "로고 이미지 URL은 500자 이하여야 합니다.")
    private String logoImageUrl;
}
