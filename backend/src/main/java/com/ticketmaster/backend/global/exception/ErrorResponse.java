package com.ticketmaster.backend.global.exception;

import lombok.Getter;

/**
 * 모든 실패 응답이 이 형태로 내려감
 * <p>
 * {
 * "code": "DUPLICATE_EMAIL",
 * "message": "이미 사용 중인 이메일입니다."
 * }
 */
@Getter
public class ErrorResponse {

    private final String code;
    private final String message;

    private ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** ErrorCode의 기본 메시지로 응답 생성 */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    /** 커스텀 메시지로 응답 생성 (상황별 추가 정보 전달용) */
    public static ErrorResponse of(ErrorCode errorCode, String customMessage) {
        return new ErrorResponse(errorCode.getCode(), customMessage);
    }
}
