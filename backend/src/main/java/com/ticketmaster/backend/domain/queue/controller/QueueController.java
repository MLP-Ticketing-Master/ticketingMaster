package com.ticketmaster.backend.domain.queue.controller;

import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.service.QueueService;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대기열 진입 / 상태 조회
 * <p>
 * POST /queue/{matchId}/enter — 대기열 진입
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/{matchId}/enter")
    @PreAuthorize("hasRole('USER')")    // 로그인 + USER 권한 필요
    public QueueEnterResponse enter(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUser().getId();
        return queueService.enter(matchId, userId);
    }
}
