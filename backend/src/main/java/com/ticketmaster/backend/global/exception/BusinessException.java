package com.ticketmaster.backend.global.exception;

import lombok.Getter;

import java.util.List;

/**
 * 모든 비즈니스 예외는 이 한 클래스로 통일
 * <p>
 * 사용 예:
 * throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED);
 * throw new BusinessException(ErrorCode.INVALID_INPUT, "이메일 형식 오류");
 * throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED, conflictSeatIds);
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 좌석 점유 충돌 시 클라이언트에게 충돌 좌석 ID를 알리기 위한 옵션 필드 */
    private final List<Long> conflictSeatIds;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.conflictSeatIds = null;
    }

    /** 메시지를 상황별로 더 자세히 적어야 할 때 */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.conflictSeatIds = null;
    }

    /**
     * 좌석 점유 충돌 — conflictSeatIds 함께 던지기
     */
    public BusinessException(ErrorCode errorCode, List<Long> conflictSeatIds) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.conflictSeatIds = conflictSeatIds;
    }
}
