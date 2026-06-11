package com.ticketmaster.backend.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

/**
 * 모든 실패 응답은 이 형태로 내려감
 *
 * 일반 응답:
 *   { "code": "DUPLICATE_EMAIL", "message": "이미 사용 중인 이메일입니다." }
 *
 * 좌석 충돌(409) 응답 :
 *   { "code": "SEAT_ALREADY_RESERVED",
 *     "message": "이미 선점된 좌석이 포함되어 있습니다.",
 *     "conflictSeatIds": [12, 15] }
 */
@Getter
public class ErrorResponse {

    private final String code;
    private final String message;

    /** 좌석 충돌 시에만 포함. null이면 JSON에서 제외됨 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<Long> conflictSeatIds;

    private ErrorResponse(String code, String message, List<Long> conflictSeatIds) {
        this.code = code;
        this.message = message;
        this.conflictSeatIds = conflictSeatIds;
    }

    /** ErrorCode의 기본 메시지로 응답 생성 */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 커스텀 메시지로 응답 생성 (상황별 추가 정보 전달용) */
    public static ErrorResponse of(ErrorCode errorCode, String customMessage) {
        return new ErrorResponse(errorCode.getCode(), customMessage, null);
    }

    /** 좌석 충돌 응답 */
    public static ErrorResponse ofConflict(ErrorCode errorCode, List<Long> conflictSeatIds) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), conflictSeatIds);
    }
}
