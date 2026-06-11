package com.ticketmaster.backend.domain.queue.controller;

import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.dto.response.QueueStatusResponse;
import com.ticketmaster.backend.domain.queue.service.QueueService;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 대기열 진입 / 상태 조회
 * <p>
 * POST /queue/{matchId}/enter — 대기열 진입
 */
@Tag(name = "대기열 API", description = "회차 대기열 진입 / 상태 폴링")
@RestController
@RequiredArgsConstructor
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;

    /**
     * 대기열 진입
     *
     * 회차에 줄을 서고 Queue-Token 발급. 응답에 토큰 + 첫 순번 동봉
     * 프론트는 받은 토큰을 헤더 Queue-Token 으로 들고 status 폴링 시작
     */
    @Operation(summary = "대기열 진입", description = "회차에 줄을 서고 Queue-Token 발급 + 첫 순번 반환")
    @PostMapping("/{matchId}/enter")
    @PreAuthorize("hasRole('USER')")    // 로그인 + USER 권한 필요
    public QueueEnterResponse enter(
            @Parameter(description = "회차 ID") @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUser().getId();
        return queueService.enter(matchId, userId);
    }

    /**
     * 대기열 상태 조회
     * <p>
     * 헤더의 Queue-Token 으로 현재 상태(WAITING / ALLOWED / EXPIRED) 반환
     * 프론트가 3초마다 폴링해서 순번 갱신 / 입장 가능 시점 감지
     *
     * @param matchId 회차 ID
     * @param token   Queue-Token 헤더 값 (없으면 null)
     */
    @Operation(summary = "대기열 상태 조회", description = "Queue-Token 으로 WAITING / ALLOWED / EXPIRED 상태 반환")
    @GetMapping("/{matchId}/status")
    @PreAuthorize("hasRole('USER')")
    public QueueStatusResponse getStatus(
            @Parameter(description = "회차 ID") @PathVariable Long matchId,
            @Parameter(description = "발급받은 대기열 토큰", in = ParameterIn.HEADER)
            @RequestHeader(value = "Queue-Token", required = false) String token
    ) {
        return queueService.getStatus(matchId, token);
    }
}
