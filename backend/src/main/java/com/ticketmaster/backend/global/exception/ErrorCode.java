package com.ticketmaster.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ====== COMMON ======
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),

    // ====== AUTH / USER ======
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "유효하지 않은 재설정 토큰입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "현재 비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    DELETED_USER(HttpStatus.FORBIDDEN, "DELETED_USER", "탈퇴 처리된 회원입니다."),

    // ====== EVENT / MATCH / TEAM ======
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "존재하지 않는 대회입니다."),
    EVENT_IN_USE(HttpStatus.CONFLICT, "EVENT_IN_USE", "진행 중인 예매가 있는 대회는 삭제할 수 없습니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "유효하지 않은 상태값입니다."),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCH_NOT_FOUND", "존재하지 않는 회차입니다."),
    MATCH_IN_USE(HttpStatus.CONFLICT, "MATCH_IN_USE", "예매가 진행된 회차는 삭제할 수 없습니다."),
    MATCH_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "MATCH_ALREADY_DELETED", "이미 삭제된 회차입니다"),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "존재하지 않는 팀입니다."),
    TEAM_IN_USE(HttpStatus.CONFLICT, "TEAM_IN_USE", "진행 중인 회차에 배정된 팀은 삭제할 수 없습니다."),
    DUPLICATE_EVENT_TITLE(HttpStatus.CONFLICT, "DUPLICATE_EVENT_TITLE", "이미 존재하는 이벤트 타이틀입니다."),
    DUPLICATE_TEAM_NAME(HttpStatus.CONFLICT, "DUPLICATE_TEAM_NAME", "이미 존재하는 팀명입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "종료일은 시작일보다 앞설 수 없습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE", "종료 시간은 시작 시간보다 앞설 수 없습니다."),
    INVALID_MATCH_DATE(HttpStatus.BAD_REQUEST, "INVALID_MATCH_DATE", "대회 기간을 벗어난 회차입니다"),
    CANNOT_CHANGE_FINISHED_MATCH(HttpStatus.BAD_REQUEST, "CANNOT_CHANGE_FINISHED_MATCH", "이미 종료된 매치는 상태를 변경할 수 없습니다."),

    // ====== SEAT / SECTION / GRADE ======
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT_NOT_FOUND", "존재하지 않는 좌석입니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "SEAT_ALREADY_RESERVED", "이미 선점된 좌석이 포함되어 있습니다."),
    SEAT_ALREADY_SOLD(HttpStatus.CONFLICT, "SEAT_ALREADY_SOLD", "이미 판매 완료된 좌석이 포함되어 있습니다."),
    SEAT_NOT_RESERVED(HttpStatus.BAD_REQUEST, "SEAT_NOT_RESERVED", "선점되지 않은 좌석은 예매할 수 없습니다."),
    SEAT_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT_RESERVATION_NOT_FOUND", "선점 정보가 존재하지 않습니다."),
    SEAT_RESERVATION_EXPIRED(HttpStatus.GONE, "SEAT_RESERVATION_EXPIRED", "선점 시간이 만료되었습니다."),
    INVALID_SEAT_STATUS(HttpStatus.BAD_REQUEST, "INVALID_SEAT_STATUS", "허용되지 않는 좌석 상태 변경입니다."),
    SEAT_NOT_EDITABLE(HttpStatus.CONFLICT, "SEAT_NOT_EDITABLE", "판매된 좌석은 수정할 수 없습니다."),
    SEAT_NOT_DELETABLE(HttpStatus.CONFLICT, "SEAT_NOT_DELETABLE", "판매되었거나 선점된 좌석은 삭제할 수 없습니다."),
    DUPLICATE_SEAT_CODE(HttpStatus.CONFLICT, "DUPLICATE_SEAT_CODE", "이미 존재하는 좌석 코드입니다."),
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SECTION_NOT_FOUND", "존재하지 않는 구역입니다."),
    SECTION_IN_USE(HttpStatus.CONFLICT, "SECTION_IN_USE", "좌석이 배정된 구역은 삭제할 수 없습니다."),
    SEAT_GRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT_GRADE_NOT_FOUND", "존재하지 않는 좌석 등급입니다."),
    SEAT_GRADE_IN_USE(HttpStatus.CONFLICT, "SEAT_GRADE_IN_USE", "좌석에 배정된 등급은 삭제할 수 없습니다."),
    DUPLICATE_GRADE_CODE(HttpStatus.CONFLICT, "DUPLICATE_GRADE_CODE", "이미 존재하는 등급 코드입니다."),
    DUPLICATE_SECTION_NAME(HttpStatus.CONFLICT, "DUPLICATE_SECTION_NAME", "동일한 이름의 구역이 이미 존재합니다."),
    DUPLICATE_SECTION_DISPLAY_ORDER(HttpStatus.CONFLICT, "DUPLICATE_SECTION_DISPLAY_ORDER", "동일한 표시 순서의 구역이 이미 존재합니다."),

    // ====== QUEUE ======
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "QUEUE_NOT_FOUND", "대기열 정보를 찾을 수 없습니다."),
    QUEUE_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "QUEUE_TOKEN_NOT_FOUND", "대기열 토큰이 유효하지 않습니다."),
    QUEUE_ALREADY_ENTERED(HttpStatus.CONFLICT, "QUEUE_ALREADY_ENTERED", "이미 대기열에 진입한 사용자입니다."),
    QUEUE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "QUEUE_ACCESS_DENIED", "대기열 접근이 거부되었습니다."),
    QUEUE_NOT_PASSED(HttpStatus.FORBIDDEN, "QUEUE_NOT_PASSED", "아직 대기열을 통과하지 못했습니다."),

    // ====== BOOKING ======
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "존재하지 않는 예매입니다."),
    BOOKING_NOT_OPEN(HttpStatus.BAD_REQUEST, "BOOKING_NOT_OPEN", "현재 예매 가능한 시간이 아닙니다."),
    BOOKING_NOT_PENDING(HttpStatus.BAD_REQUEST, "BOOKING_NOT_PENDING", "결제 가능한 예매 상태가 아닙니다."),
    BOOKING_ALREADY_CANCELED(HttpStatus.CONFLICT, "BOOKING_ALREADY_CANCELED", "이미 취소된 예매입니다."),
    BOOKING_TIME_EXPIRED(HttpStatus.GONE, "BOOKING_TIME_EXPIRED", "예매 가능 시간이 만료되었습니다."),
    BOOKING_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "BOOKING_CANNOT_CANCEL", "취소할 수 없는 예매입니다."),
    BOOKING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "BOOKING_ACCESS_DENIED", "본인의 예매만 조회/처리할 수 있습니다."),
    CANCEL_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "CANCEL_DEADLINE_PASSED", "취소 가능 기한이 지났습니다."),
    MAX_TICKETS_EXCEEDED(HttpStatus.BAD_REQUEST, "MAX_TICKETS_EXCEEDED", "1인당 예매 가능 매수를 초과했습니다."),

    // ====== PAYMENT ======
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "존재하지 않는 결제입니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_FAILED", "결제에 실패했습니다."),
    PAYMENT_TIME_EXPIRED(HttpStatus.GONE, "PAYMENT_TIME_EXPIRED", "결제 가능 시간이 만료되었습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "PAYMENT_ALREADY_COMPLETED", "이미 처리된 결제입니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_AMOUNT", "결제 금액이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
