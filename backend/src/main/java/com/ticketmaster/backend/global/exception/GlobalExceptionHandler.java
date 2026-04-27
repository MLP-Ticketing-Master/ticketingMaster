package com.ticketmaster.backend.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러에서 던져지는 예외를 한 곳에서 잡아 동일한 ErrorResponse 로 변환
 *
 * 분기:
 *  1) BusinessException                 → ErrorCode 그대로 응답
 *  2) MethodArgumentNotValidException   → @Valid 실패 (요청 DTO 검증)
 *  3) BindException                     → 쿼리 파라미터/폼 바인딩 실패
 *  4) AuthenticationException           → 인증 실패 (로그인 안 됨/토큰 오류)
 *  5) AccessDeniedException             → 인가 실패 (권한 없음)
 *  6) Exception                         → 그 외 알 수 없는 서버 오류
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 1) 비즈니스 예외 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[BusinessException] {} - {}", code.getCode(), e.getMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ErrorResponse.of(code, e.getMessage()));
    }

    /** 2) @Valid 검증 실패 (RequestBody) - 첫 번째 에러 메시지만 응답 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message));
    }

    /** 3) 쿼리 파라미터/폼 바인딩 실패 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message));
    }

    /** 4) Spring Security 인증 실패 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        log.warn("[AuthenticationException] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED));
    }

    /** 5) Spring Security 인가 실패 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AccessDeniedException] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN));
    }

    /** 6) 예상하지 못한 서버 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("[UnhandledException] ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
