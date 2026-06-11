package com.ticketmaster.backend.admin.team.service;

import com.ticketmaster.backend.admin.team.dto.request.AdminTeamCreateRequest;
import com.ticketmaster.backend.admin.team.dto.request.AdminTeamUpdateRequest;
import com.ticketmaster.backend.admin.team.dto.response.AdminTeamResponse;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.match.repository.MatchRepository;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.team.repository.TeamRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTeamService {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    /** 팀 목록 조회 (sportType이 null이면 전체 조회, 삭제되지 않은 팀만) */
    public List<AdminTeamResponse> getTeams(SportType sportType) {
        List<Team> teams = (sportType == null)
                ? teamRepository.findAllByOrderByCreatedAtDesc()
                : teamRepository.findAllBySportTypeOrderByCreatedAtDesc(sportType);

        return teams.stream()
                .map(AdminTeamResponse::from)
                .toList();
    }

    /** 팀 등록 */
    @Transactional
    public AdminTeamResponse createTeam(AdminTeamCreateRequest req) {
        // 삭제된 행까지 포함하여 동일 이름 조회
        Optional<Team> existing = teamRepository.findByNameIncludingDeleted(req.getName());

        if (existing.isPresent()) {
            Team team = existing.get();
            if (!team.isDeleted()) {
                // 활성 상태면 진짜 중복
                throw new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME);
            }
            // 삭제된 행 → 복구 + 값 갱신
            team.restore();
            team.update(req.getName(), req.getLogoImageUrl(), req.getSportType());
            return AdminTeamResponse.from(team);
        }

        // 신규 생성
        Team team = teamRepository.save(req.toEntity());
        return AdminTeamResponse.from(team);
    }

    /** 팀 부분 수정 (null이 아닌 필드만 변경) */
    @Transactional
    public AdminTeamResponse updateTeam(Long teamId, AdminTeamUpdateRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 이름을 변경하려는 경우에만 중복 체크 (자기 자신과 동일한 이름은 허용)
        if (req.getName() != null && !req.getName().equals(team.getName())) {
            if (teamRepository.existsByName(req.getName())) {
                throw new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME);
            }
        }

        team.update(req.getName(), req.getLogoImageUrl(), req.getSportType());
        return AdminTeamResponse.from(team);
    }

    /** 팀 삭제 (soft delete) */
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 진행 중 회차에 배정된 팀은 삭제 불가
        if (matchRepository.existsByTeamIdAndStatusInProgress(teamId)) {
            throw new BusinessException(ErrorCode.TEAM_IN_USE);
        }

        team.softDelete();
    }
}
