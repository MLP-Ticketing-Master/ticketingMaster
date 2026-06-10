package com.ticketmaster.backend.admin.match.controller;

import com.ticketmaster.backend.admin.match.dto.request.AdminMatchCreateRequest;
import com.ticketmaster.backend.admin.match.dto.request.AdminMatchUpdateRequest;
import com.ticketmaster.backend.admin.match.dto.response.AdminMatchResponse;
import com.ticketmaster.backend.admin.match.service.AdminMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 매치", description = "관리자 매치(회차) 등록 / 조회 / 수정 / 삭제")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMatchController {
    private final AdminMatchService matchService;

    /**
     * 전체 매치 목록 조회 (이벤트별 필터링 옵션 추가)
     */
    @Operation(summary = "매치 목록 조회", description = "전체 매치 목록 — 이벤트 ID 필터 (선택) + 페이지네이션")
    @GetMapping("/matches")
    public ResponseEntity<Page<AdminMatchResponse>> getMatchList(
            @Parameter(description = "이벤트 ID 필터 (선택)") @RequestParam(required = false) Long eventId, // 👈 필수값이 아니도록 설정 (null 허용)
            Pageable pageable) {
        return ResponseEntity.ok(matchService.getMatchList(eventId, pageable));
    }

    /**
     * 매치 등록
     */
    @Operation(summary = "매치 등록", description = "특정 이벤트에 신규 매치(회차) 생성")
    @PostMapping("/events/{eventId}/matches")
    public ResponseEntity<AdminMatchResponse> createMatch(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId,
            @Valid @RequestBody AdminMatchCreateRequest request) {
        AdminMatchResponse response = matchService.createMatch(eventId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 매치 상세 조회 (수정페이지 들어갈때)
     */
    @Operation(summary = "매치 상세 조회", description = "매치 ID로 상세 정보 반환 (수정 페이지 진입용)")
    @GetMapping("/matches/{matchId}")
    public ResponseEntity<AdminMatchResponse> getMatchDetail(
            @Parameter(description = "매치 ID") @PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getMatchDetail(matchId));
    }

    /**
     * 특정 매치 수정 요청
     */
    @Operation(summary = "매치 수정", description = "매치 정보 부분 수정")
    @PatchMapping("/matches/{matchId}")
    public ResponseEntity<AdminMatchResponse> updateMatch(
            @Parameter(description = "매치 ID") @PathVariable Long matchId,
            @Valid @RequestBody AdminMatchUpdateRequest request
            ) {
        AdminMatchResponse response = matchService.updateMatch(matchId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 매치 삭제 요청
     */
    @Operation(summary = "매치 삭제", description = "매치 삭제 (소프트 삭제)")
    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Void> deleteMatch(@Parameter(description = "매치 ID") @PathVariable Long matchId) {
        matchService.deleteMatch(matchId);

        return ResponseEntity.noContent().build();
    }
}
