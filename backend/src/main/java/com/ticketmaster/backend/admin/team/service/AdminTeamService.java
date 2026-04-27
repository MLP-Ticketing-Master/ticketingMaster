package com.ticketmaster.backend.admin.team.service;

import com.ticketmaster.backend.admin.team.dto.request.AdminTeamCreateRequest;
import com.ticketmaster.backend.admin.team.dto.request.AdminTeamUpdateRequest;
import com.ticketmaster.backend.admin.team.dto.response.AdminTeamResponse;
import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.team.entity.Team;
import com.ticketmaster.backend.domain.team.repository.AdminTeamRepository;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTeamService {
    private final AdminTeamRepository teamRepository;

    @Transactional
    public AdminTeamResponse createTeam(AdminTeamCreateRequest request) {
        // 동일한 팀명이 이미 존재하는지 확인
        if (teamRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME);
        }

        // DTO → Entity 변환 후 저장
        Team team = teamRepository.save(request.toEntity());

        // Entity → Response DTO 변환 후 반환
        return AdminTeamResponse.from(team);
    }




    public List<AdminTeamResponse> getTeams(SportType sportType) {
        // 종목 필터 유무에 따라 다른 쿼리 메서드 호출
        List<Team> teams = (sportType == null)
                ? teamRepository.findAllByOrderByCreatedAtDesc()
                : teamRepository.findAllBySportTypeOrderByCreatedAtDesc(sportType);

        // 엔티티 리스트를 응답 DTO 리스트로 변환
        return teams.stream()
                .map(AdminTeamResponse::from)
                .toList();
    }



    @Transactional
    public AdminTeamResponse updateTeam(Long teamId, AdminTeamUpdateRequest request) {
        // 대상 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 이름을 변경하려는 경우에만 중복 체크
        // (현재 자기 이름과 동일한 이름으로 수정 요청 시에는 체크 스킵)
        if (request.getName() != null && !request.getName().equals(team.getName())) {
            if (teamRepository.existsByName(request.getName())) {
                throw new BusinessException(ErrorCode.DUPLICATE_TEAM_NAME);
            }
        }
        team.update(request.getName(), request.getLogoImageUrl(), request.getSportType());

        return AdminTeamResponse.from(team);
    }



    /** 팀 삭제 (소프트 삭제) - 이벤트 대상 팀 조회 후 없으면 예외처리*/
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // TODO [MatchRepository 머지 후 활성화]
        // if (matchRepository.existsByTeamAndStatusInProgress(team)) {
        //     throw new BusinessException(ErrorCode.TEAM_IN_USE);
        // }

        // @SQLDelete 어노테이션 덕분에 실제로는 UPDATE 쿼리가 실행됨
        // (deleted_at 컬럼에 현재 시각이 기록됨)

        teamRepository.delete(team);
    }

}
