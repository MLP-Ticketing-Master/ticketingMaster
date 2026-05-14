package com.ticketmaster.backend.domain.queue.controller;

import com.ticketmaster.backend.domain.queue.dto.response.QueueEnterResponse;
import com.ticketmaster.backend.domain.queue.service.QueueService;
import com.ticketmaster.backend.domain.user.entity.User;
import com.ticketmaster.backend.global.config.SecurityConfig;
import com.ticketmaster.backend.global.security.auth.CustomUserDetails;
import com.ticketmaster.backend.global.security.auth.CustomUserDetailsService;
import com.ticketmaster.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TK-82 대기열 진입 컨트롤러 슬라이스 테스트
 */
@WebMvcTest(QueueController.class)
@Import(SecurityConfig.class)
@DisplayName("대기열 진입 컨트롤러 슬라이스 테스트")
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("TC-05: 정상 응답 → 200 + queueToken 포함")
    void 정상_응답() throws Exception {
        // given - 인증된 사용자
        CustomUserDetails principal = mockedPrincipal(1000L);
        QueueEnterResponse response = QueueEnterResponse.of(
                "test-token-abc", 1L, 0L, 0L, LocalDateTime.of(2026, 1, 1, 10, 0));
        given(queueService.enter(eq(1L), eq(1000L))).willReturn(response);

        // when & then
        mockMvc.perform(post("/queue/1/enter").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueToken").value("test-token-abc"));
    }

    @Test
    @DisplayName("TC-06: 비로그인 호출 → 401")
    void 비로그인_차단() throws Exception {
        // given — 인증 정보 없음

        // when & then
        mockMvc.perform(post("/queue/1/enter"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-07: 응답 필드 모두 정상 반환")
    void 응답_필드_검증() throws Exception {
        // given — 모든 필드 채운 응답을 service mock 이 돌려주도록 설정
        CustomUserDetails principal = mockedPrincipal(1000L);
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);
        QueueEnterResponse response = QueueEnterResponse.of(
                "test-token", 50L, 49L, 7L, now);
        given(queueService.enter(eq(1L), eq(1000L))).willReturn(response);

        // when & then — JSON 직렬화된 모든 필드 확인
        mockMvc.perform(post("/queue/1/enter").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueToken").value("test-token"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.queueNumber").value(50))
                .andExpect(jsonPath("$.remainingAhead").value(49))
                .andExpect(jsonPath("$.estimatedWaitSeconds").value(7))
                .andExpect(jsonPath("$.enteredAt").exists());
    }

    // ──────── 헬퍼 ────────────────────────────────────────

    private CustomUserDetails mockedPrincipal(Long userId) {
        User user = User.create("test@test.com", "encoded", "tester", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return new CustomUserDetails(user);
    }
}