package com.ticketmaster.backend.admin.match.dto.request;

import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매치 수정 DTO (PATCH /admin/matches/{matchId})
 */
@Getter
@Builder
public class AdminMatchUpdateRequest {
    private String roundLabel;
    private Long homeTeamId;
    private Long awayTeamId;
    private LocalDate matchDate;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private MatchStatus status;
}
