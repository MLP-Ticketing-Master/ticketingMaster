package com.ticketmaster.backend.admin.match.dto.request;

import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매치 등록 DTO (POST /admin/events/{eventId}/matches)
 */
@Getter
public class AdminMatchCreateRequest {
    @NotBlank(message = "회차 라벨(=매치 타이틀)은 필수입니다.")
    private String roundLabel;

    @NotNull(message = "경기 날짜는 필수입니다.")
    private LocalDate matchDate;

    @NotNull(message = "시작 시간은 필수입니다.")
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Long homeTeamId; // NULL 허용 (대진 미정일 수 있음)
    private Long awayTeamId;
}
