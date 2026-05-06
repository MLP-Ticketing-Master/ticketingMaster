package com.ticketmaster.backend.admin.match.service;

import com.ticketmaster.backend.admin.match.dto.request.AdminMatchCreateRequest;
import com.ticketmaster.backend.admin.match.dto.request.AdminMatchUpdateRequest;
import com.ticketmaster.backend.admin.match.dto.response.AdminMatchResponse;
import com.ticketmaster.backend.domain.event.entity.Event;
import com.ticketmaster.backend.domain.event.repository.EventRepository;
import com.ticketmaster.backend.domain.match.entity.Match;
import com.ticketmaster.backend.domain.match.entity.MatchStatus;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.team.repository.AdminTeamRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMatchService {
    private final MatchRepository matchRepo;
    private final EventRepository eventRepo;
    private final AdminTeamRepository teamRepo;

    /**
     * 전체 매치 목록 조회
     */
    public Page<AdminMatchResponse> getMatchList(Pageable pageable) {
        Page<Match> matchPage = matchRepo.findAll(pageable);

        return matchPage.map(AdminMatchResponse::from);
    }

    /**
     * 매치 등록
     */
    @Transactional
    public AdminMatchResponse createMatch(Long eventId, AdminMatchCreateRequest request) {
        // 예외-1) 없는 이벤트인 경우
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 예외-2) 존재하지 않는 팀인 경우
        Team homeTeam = null;
        if (request.getHomeTeamId() != null) {
            homeTeam = teamRepo.findById(request.getHomeTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        }

        Team awayTeam = null;
        if (request.getAwayTeamId() != null) {
            awayTeam = teamRepo.findById(request.getAwayTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        }

        // 예외-3) 시간 오류 (ex: 끝나는 시간이 시작 시간보다 앞선 경우)
        if (request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }
        // TODO: INVALID_MATCH_DATE (대회 기간을 벗어난 회차 예외 추가)

        // 엔티티 생성
        Match match = buildMatchEntity(request, event, homeTeam, awayTeam);

        // DB 저장
        Match saved = matchRepo.save(match);
        return AdminMatchResponse.from(saved);
    }

    /**
     * 특정 매치 상세 조회 (수정페이지 들어갈때)
     */
    public AdminMatchResponse getMatchDetail(Long id) {
        Match match = matchRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        return AdminMatchResponse.from(match);
    }

    /**
     * 특정 매치 수정 요청
     */
    @Transactional
    public AdminMatchResponse updateMatch(Long id, AdminMatchUpdateRequest request) {
        Match match = matchRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 이미 삭제된 것에 대한 수정 방지 처리
        if (match.getDeletedAt() != null) { throw new BusinessException(ErrorCode.MATCH_ALREADY_DELETED); }

        // 예외-1) 존재하지 않는 팀인 경우
        Team homeTeam = null;
        if (request.getHomeTeamId() != null) {
            homeTeam = teamRepo.findById(request.getHomeTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        }

        Team awayTeam = null;
        if (request.getAwayTeamId() != null) {
            awayTeam = teamRepo.findById(request.getAwayTeamId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        }

        // 예외-2) 시간 오류 (ex: 끝나는 시간이 시작 시간보다 앞선 경우)
        LocalDateTime newStart = request.getStartAt() != null ? request.getStartAt() : match.getStartAt();
        LocalDateTime newEnd = request.getEndAt() != null ? request.getEndAt() : match.getEndAt();

        if (newEnd != null && newEnd.isBefore(newStart)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }
        // TODO: INVALID_MATCH_DATE (대회 기간을 벗어난 회차 예외 추가)


        match.update(request, homeTeam, awayTeam); // ← pass resolved teams in
        return AdminMatchResponse.from(match);
    }


    /**
     *  엔티티 생성 메소드
     */
    private Match buildMatchEntity(AdminMatchCreateRequest req, Event event, Team home, Team away) {
        return Match.builder()
                .event(event)
                .roundLabel(req.getRoundLabel())
                .matchDate(req.getMatchDate())
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .homeTeam(home)
                .awayTeam(away)
                .status(MatchStatus.SCHEDULED)
                .build();
    }

    /**
     * 특정 매치 삭제 요청
     */
    @Transactional
    public void deleteMatch(Long id) {
        Match match = matchRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        // 이미 삭제된 것에 대한 중복 삭제 방지 처리
        if (match.getDeletedAt() != null) { throw new BusinessException(ErrorCode.MATCH_ALREADY_DELETED); }

        // 예외-1) 예매 이력이 존재하는 매치인 경우

        match.softDelete();
    }
}
