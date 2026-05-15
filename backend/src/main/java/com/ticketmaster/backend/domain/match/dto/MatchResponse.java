package com.ticketmaster.backend.domain.match.dto;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.team.dto.TeamResponse;
import com.ticketmaster.backend.domain.team.entity.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MatchResponse {
    private Long matchId;
    private String roundLabel;
    private LocalDate matchDate;
    private LocalDateTime startAt;
    private TeamResponse homeTeam;
    private TeamResponse awayTeam;
    private MatchStatus status;

    // TODO: isBookable 필드 생성

    // Entity -> DTO
    public static MatchResponse from(Match match) {
        return MatchResponse.builder()
                .matchId(match.getId())
                .roundLabel(match.getRoundLabel())
                .matchDate(match.getMatchDate())
                .startAt(match.getStartAt())
                .homeTeam(TeamResponse.from(match.getHomeTeam()))
                .awayTeam(TeamResponse.from(match.getAwayTeam()))
                .status(match.getStatus())
                // .isBookable(match.isBookable(LocalDateTime.now())) // TODO
                .build();
    }
}
