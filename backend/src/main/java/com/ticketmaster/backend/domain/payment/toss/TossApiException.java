package com.ticketmaster.backend.domain.payment.toss;

/**
 * 토스 API 호출 실패 시 throw
 * GlobalExceptionHandler 가 ErrorCode.TOSS_API_ERROR (502) 로 매핑
 */
public class TossApiException extends RuntimeException {
    public TossApiException(String message) {
        super(message);
    }

    public TossApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
