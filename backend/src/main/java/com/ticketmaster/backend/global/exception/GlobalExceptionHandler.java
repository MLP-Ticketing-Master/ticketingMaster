package com.ticketmaster.backend.global.exception;

import com.ticketmaster.backend.domain.payment.toss.TossApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러에서 던져지는 예외를 한 곳에서 잡아 동일한 ErrorResponse 로 변환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1) 비즈니스 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[BusinessException] {} - {}", code.getCode(), e.getMessage());

        ErrorResponse body = (e.getConflictSeatIds() != null)
                ? ErrorResponse.ofConflict(code, e.getConflictSeatIds())
                : ErrorResponse.of(code, e.getMessage());

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(body);
    }

    /**
     * 2) @Valid 검증 실패 (RequestBody) - 첫 번째 에러 메시지만 응답
     */
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

    /**
     * 3) 쿼리 파라미터/폼 바인딩 실패
     */
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

    /**
     * 4) Spring Security 인증 실패
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        log.warn("[AuthenticationException] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 5) Spring Security 인가 실패
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AccessDeniedException] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN));
    }

    /**
     * 6) DB UNIQUE 위반 — BookingSeat (match_id, seat_id) 중복 예매 차단
     * BookingService 에서 BookingSeat 저장 시 발생 → SEAT_ALREADY_RESERVED (409)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("[DataIntegrityViolationException] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.SEAT_ALREADY_RESERVED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.SEAT_ALREADY_RESERVED));
    }

    /**
     * 7) 토스 API 호출 자체 실패 (네트워크 / 인증 / 토스 서버 오류)
     */
    @ExceptionHandler(TossApiException.class)
    public ResponseEntity<ErrorResponse> handleTossApi(TossApiException e) {
        log.error("[TossApiException] {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.TOSS_API_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.TOSS_API_ERROR));
    }

    /**
     * 8) 예상하지 못한 서버 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("[UnhandledException] ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}