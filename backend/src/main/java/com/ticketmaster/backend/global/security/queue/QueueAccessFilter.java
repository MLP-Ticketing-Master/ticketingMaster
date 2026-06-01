package com.ticketmaster.backend.global.security.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmaster.backend.domain.queue.util.QueueTokenValidator;
import com.ticketmaster.backend.global.exception.BusinessException;
import com.ticketmaster.backend.global.exception.ErrorCode;
import com.ticketmaster.backend.global.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 좌석 점유/해제 API 요청 시 Queue-Token 헤더의 ALLOWED 여부를 검증하는 필터
 *
 * 흐름
 *  1) URL 정규식으로 matchId 추출 — 대상 URL 이 아니면 통과
 *  2) Queue-Token 헤더 추출 후 QueueTokenValidator 위임
 *  3) 검증 실패 시 GlobalExceptionHandler 와 동일한 ErrorResponse JSON 형식으로 직접 응답
 *  4) 통과 시 다음 필터 / 컨트롤러로 진행
 *
 * 등록은 QueueFilterConfig 의 FilterRegistrationBean 에서 처리
 */
@Slf4j
@RequiredArgsConstructor
public class QueueAccessFilter extends OncePerRequestFilter {

    // /matches/{matchId}/seats 로 시작하는 경로에서 matchId 캡처
    private static final Pattern MATCH_ID_PATTERN =
            Pattern.compile("^/matches/(\\d+)/seats(?:/.*)?$");

    private static final String HEADER_QUEUE_TOKEN = "Queue-Token";

    private final QueueTokenValidator queueTokenValidator;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 대상 URL 이 아니거나, 좌석 해제(DELETE) 요청이면 큐 검증 스킵
        // release 는 본인 점유 좌석을 돌려주는 행위라 시스템 보호 관점에서 큐 통과 요구 불필요
        // 본인 점유 검증은 SeatReservationService.release() 내부에서 처리
        Matcher matcher = MATCH_ID_PATTERN.matcher(request.getRequestURI());
        if (!matcher.matches() || "DELETE".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Long matchId = Long.parseLong(matcher.group(1));
        String queueToken = request.getHeader(HEADER_QUEUE_TOKEN);

        log.debug("[QueueAccessFilter] 검증 시작 - matchId={}, uri={}, hasToken={}",
                matchId, request.getRequestURI(), queueToken != null);

        try {
            // 토큰 누락 / ALLOWED 아님 => 두 케이스를 validator 가 모두 분기
            queueTokenValidator.validateAllowed(matchId, queueToken);
        } catch (BusinessException e) {
            writeError(response, e.getErrorCode());
            return;
        }

        // 토큰 검증을 통과한 정상 요청을 컨트롤러로 보냄
        chain.doFilter(request, response);
    }

    /**
     * Filter 단계는 @RestControllerAdvice 가 도달하지 못하므로
     * ErrorResponse 를 직접 직렬화해서 동일 포맷 유지
     */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        log.warn("[QueueAccessFilter] {} - {}", errorCode.getCode(), errorCode.getMessage());

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
