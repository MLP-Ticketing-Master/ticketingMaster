package com.ticketmaster.backend.admin.team.dto.response;

import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.team.entity.Team;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminTeamResponse {
    private Long teamId;
    private String name;
    private SportType sportType;
    private String logoImageUrl;


    public static AdminTeamResponse from(Team entity) {
        return new AdminTeamResponse(
            entity.getId(),
            entity.getName(),
            entity.getSportType(),
            entity.getLogoImageUrl()
        );
    }
}
