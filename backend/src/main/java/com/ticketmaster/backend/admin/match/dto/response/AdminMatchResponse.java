package com.ticketmaster.backend.admin.match.dto.response;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminMatchResponse {
    private Long id;
    private Long eventId;
    private String roundLabel;
    private Long homeTeamId;
    private Long awayTeamId;
    private LocalDate matchDate;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private MatchStatus status;

    private LocalDateTime deletedAt; // 삭제된 매치인지 확인용 (관리자는 다 보이게끔)

    // Entity -> DTO 변환 메소드
    public static AdminMatchResponse from(Match m) {
        return new AdminMatchResponse(
                m.getId(),
                m.getEvent().getId(),
                m.getRoundLabel(),
                (m.getHomeTeam() != null ? m.getHomeTeam().getId() : null), // 널처리
                (m.getAwayTeam() != null ? m.getAwayTeam().getId() : null), // 널처리
                m.getMatchDate(),
                m.getStartAt(),
                m.getEndAt(),
                m.getStatus(),
                m.getDeletedAt()
        );
    }
}
