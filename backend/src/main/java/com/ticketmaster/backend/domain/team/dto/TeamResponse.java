package com.ticketmaster.backend.domain.team.dto;

import com.ticketmaster.backend.domain.team.entity.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class TeamResponse {
    private String name;
    private String logoImageUrl;

    // Entity -> DTO
    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .name(team.getName())
                .logoImageUrl(team.getLogoImageUrl())
                .build();
    }
}
