package com.ticketmaster.backend.domain.match.entity;

import com.ticketmaster.backend.admin.match.dto.request.AdminMatchUpdateRequest;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.global.common.BaseEntity;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회차(특정 일시의 개별 경기)
 * - 좌석/대기열/예매는 Match 단위로 독립적
 */
@Getter
@Entity
@Table(name = "matches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "round_label", length = 50, nullable = false)
    private String roundLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam; // NULL 허용 (대진 미정)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MatchStatus status;

    /**
     * 생성자 (빌더)
     */
    @Builder
    public Match(Event event, String roundLabel, Team homeTeam, Team awayTeam,
                 LocalDate matchDate, LocalDateTime startAt, LocalDateTime endAt,
                 MatchStatus status) {
        this.event = event;
        this.roundLabel = roundLabel;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.matchDate = matchDate;
        this.startAt = startAt;
        this.endAt = (endAt != null) ? endAt : startAt.plusHours(2);
        this.status = (status != null) ? status : MatchStatus.SCHEDULED; // Default value logic
    }

    public void update(AdminMatchUpdateRequest request, Team homeTeam, Team awayTeam) {
        if (request.getRoundLabel() != null) this.roundLabel = request.getRoundLabel();
        if (request.getMatchDate() != null) this.matchDate = request.getMatchDate();
        if (request.getStartAt() != null) this.startAt = request.getStartAt();
        if (request.getEndAt() != null) this.endAt = request.getEndAt();
        if (request.getStatus() != null) changeStatus(request.getStatus());
        if (request.getHomeTeamId() != null) this.homeTeam = homeTeam;
        if (request.getAwayTeamId() != null) this.awayTeam = awayTeam;
    }

    public void changeStatus(MatchStatus newStatus) {
        if (this.status == newStatus) return;  // 같은 상태면 무시

        if (this.status == MatchStatus.FINISHED) {
            throw new BusinessException(ErrorCode.CANNOT_CHANGE_FINISHED_MATCH);
        }
        this.status = newStatus;
    }
}