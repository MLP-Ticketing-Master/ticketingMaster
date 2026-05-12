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
import com.ticketmaster.backend.domain.team.repository.TeamRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMatchService {
    private final MatchRepository matchRepo;
    private final EventRepository eventRepo;
    private final TeamRepository teamRepo;

    /**
     * 전체 매치 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminMatchResponse> getMatchList(Long eventId, Pageable pageable) {
        Page<Match> matchPage;

        // eventId가 파라미터로 넘어왔다면 필터링 조회, 아니면 전체 조회
        if (eventId != null) {
            matchPage = matchRepo.findByEventId(eventId, pageable);
        } else {
            matchPage = matchRepo.findAll(pageable);
        }

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

        // 예외-2) 매치 날짜가 이벤트 기간을 벗어난 경우 (DB 낭비 방지를 위해 팀 조회 전 검증!)
        LocalDate matchDate = request.getMatchDate();
        if (matchDate.isBefore(event.getStartDate()) || matchDate.isAfter(event.getEndDate())) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_DATE);
        }

        // 예외-3) 시간 오류 (ex: 끝나는 시간이 시작 시간보다 앞선 경우)
        if (request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }

        // 예외-4) 존재하지 않는 팀인 경우
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

        // 엔티티 생성 및 DB 저장
        Match match = buildMatchEntity(request, event, homeTeam, awayTeam);
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

        // 예외-1) 대회 기간 벗어난 회차 예외 처리
        LocalDate newDate = request.getMatchDate() != null ? request.getMatchDate() : match.getMatchDate();
        if (newDate.isBefore(match.getEvent().getStartDate()) || newDate.isAfter(match.getEvent().getEndDate())) {
            throw new BusinessException(ErrorCode.INVALID_MATCH_DATE);
        }

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
        LocalDateTime newStart = request.getStartAt() != null ? request.getStartAt() : match.getStartAt();
        LocalDateTime newEnd = request.getEndAt() != null ? request.getEndAt() : match.getEndAt();

        if (newEnd != null && newEnd.isBefore(newStart)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }

        match.update(request, homeTeam, awayTeam); // 엔티티 상태 변경

        // JPA 더티 체킹에 의해 트랜잭션 종료 시 자동 UPDATE 쿼리가 날아갑니다. (save 호출 불필요)
        return AdminMatchResponse.from(match);
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
}
