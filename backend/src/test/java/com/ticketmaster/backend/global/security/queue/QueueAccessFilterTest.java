package com.ticketmaster.backend.global.security.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.queue.util.QueueTokenValidator;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 좌석 API 진입 시 큐 토큰을 검증하는 필터 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 토큰 검증 필터 단위 테스트")
class QueueAccessFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private QueueTokenValidator queueTokenValidator;

    private QueueAccessFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new QueueAccessFilter(queueTokenValidator, objectMapper);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("TC-01: ALLOWED 토큰 → 다음 필터로 통과")
    void 통과() throws Exception {
        // given — 좌석 점유 요청, validator 통과
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/matches/1/seats/reserve");
        request.addHeader("Queue-Token", "allowed-token");
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(queueTokenValidator).validateAllowed(1L, "allowed-token");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("TC-02: 헤더 없음 → 401 + QUEUE_TOKEN_NOT_FOUND 응답")
    void 토큰_누락() throws Exception {
        // given — Queue-Token 헤더 없음, validator 가 토큰 누락 예외 throw
        ErrorCode expected = ErrorCode.QUEUE_TOKEN_NOT_FOUND;
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/matches/1/seats/reserve");
        willThrow(new BusinessException(expected))
                .given(queueTokenValidator).validateAllowed(anyLong(), any());
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        assertErrorResponse(expected);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("TC-03: ALLOWED 아닌 토큰 → 403 + QUEUE_NOT_PASSED 응답")
    void 권한_없음() throws Exception {
        // given — WAITING 상태 토큰, validator 가 QUEUE_NOT_PASSED 던짐
        ErrorCode expected = ErrorCode.QUEUE_NOT_PASSED;
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/matches/1/seats/reserve");
        request.addHeader("Queue-Token", "waiting-token");
        willThrow(new BusinessException(expected))
                .given(queueTokenValidator).validateAllowed(1L, "waiting-token");
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        assertErrorResponse(expected);
    }

    @Test
    @DisplayName("TC-04: 만료된 토큰 (Redis 키 없음) → 403 QUEUE_NOT_PASSED")
    void 만료_토큰() throws Exception {
        // given — 만료된 토큰, validator 가 Redis 키 부재로 QUEUE_NOT_PASSED 던짐
        ErrorCode expected = ErrorCode.QUEUE_NOT_PASSED;
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/matches/1/seats/reserve");
        request.addHeader("Queue-Token", "expired-token");
        willThrow(new BusinessException(expected))
                .given(queueTokenValidator).validateAllowed(1L, "expired-token");
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        assertErrorResponse(expected);
    }

    @Test
    @DisplayName("TC-05: 차단 대상 외 URL → validator 미호출 + 통과")
    void 차단_대상_외() throws Exception {
        // given — sections 조회는 검증 대상이 아님
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/matches/1/sections");
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(queueTokenValidator, never()).validateAllowed(any(), any());
        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-06: 차단 응답 JSON 형식 — code/message 필드 (ErrorResponse 와 동일)")
    void 응답_형식() throws Exception {
        // given — DELETE 메서드여도 동일 포맷 응답
        ErrorCode expected = ErrorCode.QUEUE_NOT_PASSED;
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/matches/2/seats/reserve");
        willThrow(new BusinessException(expected))
                .given(queueTokenValidator).validateAllowed(anyLong(), any());
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        assertErrorResponse(expected);
    }

    /**
     * 차단 응답의 status / code / message 가 ErrorCode enum 정의와 동일한지 검증
     */
    private void assertErrorResponse(ErrorCode expected) throws Exception {
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(expected.getHttpStatus().value());
        assertThat(response.getContentType()).contains("application/json");
        assertThat(body.get("code").asText()).isEqualTo(expected.getCode());
        assertThat(body.get("message").asText()).isEqualTo(expected.getMessage());
    }
}