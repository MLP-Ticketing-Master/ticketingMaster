package com.ticketmaster.backend.global.exception;

import lombok.Getter;

/**
 * 모든 비즈니스 예외는 이 한 클래스로 통일
 * <p>
 * 사용 예:
 * throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED);
 * throw new BusinessException(ErrorCode.INVALID_INPUT, "이메일 형식 오류");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 메시지를 상황별로 더 자세히 적어야 할 때 */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
