package com.ticketmaster.backend.admin.team.controller;

import com.ticketmaster.backend.admin.team.dto.request.AdminTeamCreateRequest;
import com.ticketmaster.backend.admin.team.dto.request.AdminTeamUpdateRequest;
import com.ticketmaster.backend.admin.team.dto.response.AdminTeamResponse;
import com.ticketmaster.backend.admin.team.service.AdminTeamService;
import com.ticketmaster.backend.domain.event.entity.SportType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 팀", description = "관리자 팀 조회 / 등록 / 수정 / 삭제")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeamController {

    private final AdminTeamService teamService;

    /** 팀 목록 조회 (종목 필터 가능) — 200 OK */
    @Operation(summary = "팀 목록 조회", description = "팀 목록 반환 — 종목 필터 (선택)")
    @GetMapping("/teams")
    public ResponseEntity<List<AdminTeamResponse>> getTeams(
            @Parameter(description = "종목 필터 (선택)") @RequestParam(required = false) SportType sportType) {
        return ResponseEntity.ok(teamService.getTeams(sportType));
    }

    /** 팀 등록 — 201 Created */
    @Operation(summary = "팀 등록", description = "신규 팀 생성")
    @PostMapping("/teams")
    public ResponseEntity<AdminTeamResponse> createTeam(
            @RequestBody @Valid AdminTeamCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    /** 팀 부분 수정 — 200 OK */
    @Operation(summary = "팀 수정", description = "팀 정보 부분 수정")
    @PatchMapping("/teams/{teamId}")
    public ResponseEntity<AdminTeamResponse> updateTeam(
            @Parameter(description = "팀 ID") @PathVariable Long teamId,
            @RequestBody @Valid AdminTeamUpdateRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(teamId, request));
    }

    /** 팀 삭제 (소프트 삭제) — 204 No Content */
    @Operation(summary = "팀 삭제", description = "팀 삭제 (소프트 삭제)")
    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@Parameter(description = "팀 ID") @PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.noContent().build();
    }
}
