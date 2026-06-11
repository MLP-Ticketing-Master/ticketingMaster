package com.ticketmaster.backend.admin.team.dto.request;

import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.team.entity.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminTeamCreateRequest {

    /** 팀명
     *  {@code @NotBlank}: null, 빈 문자열, 공백만 있는 문자열 모두 거부
     *  {@code @Size}: 최대 100자 (Team 엔티티 컬럼 길이와 동일) */
    @NotBlank(message = "팀명은 필수입니다.")
    @Size(max = 100, message = "팀명은 100자 이하여야 합니다.")
    private String name;

    /** 종목 (LOL, VALORANT, OVERWATCH 등).
     * {@code @NotNull}: enum 타입은 @NotBlank가 적용되지 않으므로 @NotNull 사용 */
    @NotNull(message = "종목은 필수입니다.")
    private SportType sportType;

    /** 로고 이미지 URL
     * 선택 입력값 - 입력하지 않으면 null로 저장된다 */
    @Size(max = 500, message = "로고 이미지 URL은 500자 이하여야 합니다.")
    private String logoImageUrl;

    /** Request DTO를 Team 엔티티로 변환*/
    public Team toEntity() {
        return Team.builder()
                .name(name)
                .sportType(sportType)
                .logoImageUrl(logoImageUrl)
                .build();
    }

}

