package com.ticketmaster.backend.admin.match.controller;

import com.ticketmaster.backend.admin.match.dto.request.AdminMatchCreateRequest;
import com.ticketmaster.backend.admin.match.dto.request.AdminMatchUpdateRequest;
import com.ticketmaster.backend.admin.match.dto.response.AdminMatchResponse;
import com.ticketmaster.backend.admin.match.service.AdminMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMatchController {
    private final AdminMatchService matchService;

    /**
     * 전체 매치 목록 조회
     */
    @GetMapping("/matches")
    public ResponseEntity<Page<AdminMatchResponse>> getMatchList(Pageable pageable) {
        return ResponseEntity.ok(matchService.getMatchList(pageable));
    }

    /**
     * 매치 등록
     */
    @PostMapping("/events/{eventId}/matches")
    public ResponseEntity<AdminMatchResponse> createMatch(@Valid @RequestBody AdminMatchCreateRequest request) {
        AdminMatchResponse response = matchService.createMatch(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 매치 상세 조회 (수정페이지 들어갈때)
     */
    @GetMapping("/matches/{matchId}")
    public ResponseEntity<AdminMatchResponse> getMatchDetail(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getMatchDetail(matchId));
    }

    /**
     * 특정 매치 수정 요청
     */
    @PatchMapping("/matches/{matchId}")
    public ResponseEntity<AdminMatchResponse> updateMatch(
            @PathVariable Long matchId,
            @Valid @RequestBody AdminMatchUpdateRequest request
            ) {
        AdminMatchResponse response = matchService.updateMatch(matchId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 매치 삭제 요청
     */
    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long matchId) {
        matchService.deleteMatch(matchId);

        return ResponseEntity.noContent().build();
    }
}
