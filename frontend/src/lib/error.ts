import { isAxiosError } from "axios";

/**
 * 백엔드 ErrorCode (com.ticketmaster.backend.global.exception.ErrorCode) 미러
 * - 백엔드와 1:1 동일 키/분류 유지
 * - 메시지는 백엔드 응답의 message 필드를 그대로 사용
 * - UX 카피를 다르게 보여줄 필요가 있으면 ERROR_MESSAGE_OVERRIDES 에서 override
 */
export const ERROR_CODES = {
  // ===== COMMON =====
  INVALID_INPUT: "INVALID_INPUT",
  UNAUTHORIZED: "UNAUTHORIZED",
  FORBIDDEN: "FORBIDDEN",
  INTERNAL_SERVER_ERROR: "INTERNAL_SERVER_ERROR",

  // ===== AUTH / USER =====
  DUPLICATE_EMAIL: "DUPLICATE_EMAIL",
  INVALID_CREDENTIALS: "INVALID_CREDENTIALS",
  INVALID_TOKEN: "INVALID_TOKEN",
  EXPIRED_TOKEN: "EXPIRED_TOKEN",
  INVALID_RESET_TOKEN: "INVALID_RESET_TOKEN",
  INVALID_PASSWORD: "INVALID_PASSWORD",
  USER_NOT_FOUND: "USER_NOT_FOUND",
  DELETED_USER: "DELETED_USER",

  // ===== EVENT / MATCH / TEAM =====
  EVENT_NOT_FOUND: "EVENT_NOT_FOUND",
  EVENT_IN_USE: "EVENT_IN_USE",
  INVALID_STATUS: "INVALID_STATUS",
  MATCH_NOT_FOUND: "MATCH_NOT_FOUND",
  MATCH_IN_USE: "MATCH_IN_USE",
  MATCH_ALREADY_DELETED: "MATCH_ALREADY_DELETED",
  TEAM_NOT_FOUND: "TEAM_NOT_FOUND",
  TEAM_IN_USE: "TEAM_IN_USE",
  DUPLICATE_EVENT_TITLE: "DUPLICATE_EVENT_TITLE",
  DUPLICATE_TEAM_NAME: "DUPLICATE_TEAM_NAME",
  INVALID_SORT_VALUE: "INVALID_SORT_VALUE",
  INVALID_DATE_RANGE: "INVALID_DATE_RANGE",
  INVALID_TIME_RANGE: "INVALID_TIME_RANGE",
  INVALID_MATCH_DATE: "INVALID_MATCH_DATE",
  CANNOT_CHANGE_FINISHED_MATCH: "CANNOT_CHANGE_FINISHED_MATCH",

  // ===== SEAT / SECTION / GRADE =====
  SEAT_NOT_FOUND: "SEAT_NOT_FOUND",
  SEAT_ALREADY_RESERVED: "SEAT_ALREADY_RESERVED",
  SEAT_ALREADY_SOLD: "SEAT_ALREADY_SOLD",
  SEAT_NOT_RESERVED: "SEAT_NOT_RESERVED",
  SEAT_RESERVATION_NOT_FOUND: "SEAT_RESERVATION_NOT_FOUND",
  SEAT_RESERVATION_EXPIRED: "SEAT_RESERVATION_EXPIRED",
  INVALID_SEAT_STATUS: "INVALID_SEAT_STATUS",
  SEAT_NOT_EDITABLE: "SEAT_NOT_EDITABLE",
  SEAT_NOT_DELETABLE: "SEAT_NOT_DELETABLE",
  DUPLICATE_SEAT_CODE: "DUPLICATE_SEAT_CODE",
  SECTION_NOT_FOUND: "SECTION_NOT_FOUND",
  SECTION_IN_USE: "SECTION_IN_USE",
  SEAT_GRADE_NOT_FOUND: "SEAT_GRADE_NOT_FOUND",
  SEAT_GRADE_IN_USE: "SEAT_GRADE_IN_USE",
  DUPLICATE_GRADE_CODE: "DUPLICATE_GRADE_CODE",
  DUPLICATE_SECTION_NAME: "DUPLICATE_SECTION_NAME",
  DUPLICATE_SECTION_DISPLAY_ORDER: "DUPLICATE_SECTION_DISPLAY_ORDER",

  // ===== QUEUE =====
  QUEUE_NOT_FOUND: "QUEUE_NOT_FOUND",
  QUEUE_TOKEN_NOT_FOUND: "QUEUE_TOKEN_NOT_FOUND",
  QUEUE_ACCESS_DENIED: "QUEUE_ACCESS_DENIED",
  QUEUE_NOT_PASSED: "QUEUE_NOT_PASSED",
  QUEUE_TOKEN_EXPIRED: "QUEUE_TOKEN_EXPIRED",
  QUEUE_TOKEN_MATCH_MISMATCH: "QUEUE_TOKEN_MATCH_MISMATCH",

  // ===== BOOKING =====
  BOOKING_NOT_FOUND: "BOOKING_NOT_FOUND",
  BOOKING_NOT_OPEN: "BOOKING_NOT_OPEN",
  BOOKING_NOT_PENDING: "BOOKING_NOT_PENDING",
  BOOKING_ALREADY_CANCELED: "BOOKING_ALREADY_CANCELED",
  BOOKING_TIME_EXPIRED: "BOOKING_TIME_EXPIRED",
  BOOKING_CANNOT_CANCEL: "BOOKING_CANNOT_CANCEL",
  BOOKING_ACCESS_DENIED: "BOOKING_ACCESS_DENIED",
  CANCEL_DEADLINE_PASSED: "CANCEL_DEADLINE_PASSED",
  MAX_TICKETS_EXCEEDED: "MAX_TICKETS_EXCEEDED",

  // ===== PAYMENT =====
  PAYMENT_NOT_FOUND: "PAYMENT_NOT_FOUND",
  PAYMENT_FAILED: "PAYMENT_FAILED",
  PAYMENT_TIME_EXPIRED: "PAYMENT_TIME_EXPIRED",
  PAYMENT_ALREADY_COMPLETED: "PAYMENT_ALREADY_COMPLETED",
  INVALID_PAYMENT_AMOUNT: "INVALID_PAYMENT_AMOUNT",
  TOSS_API_ERROR: "TOSS_API_ERROR",
} as const;

export type ErrorCode = (typeof ERROR_CODES)[keyof typeof ERROR_CODES];

/**
 * 큐 토큰 관련 에러(만료/누락/매치 불일치) — 한 그룹으로 묶어 에러 화면 분기
 */
export const QUEUE_TOKEN_ERROR_CODES: readonly ErrorCode[] = [
  ERROR_CODES.QUEUE_TOKEN_NOT_FOUND,
  ERROR_CODES.QUEUE_TOKEN_EXPIRED,
  ERROR_CODES.QUEUE_TOKEN_MATCH_MISMATCH,
];

/**
 * 백엔드 메시지가 기술적/딱딱한 코드에 한해서만 UX 친화 카피로 override
 * - 백엔드 message 가 이미 사용자 친화적이면 추가할 필요 없음
 * - 키만 추가하면 자동으로 resolveErrorMessage 가 우선 적용
 */
export const ERROR_MESSAGE_OVERRIDES: Partial<Record<ErrorCode, string>> = {
  INVALID_TOKEN: "다시 로그인해 주세요.",
  EXPIRED_TOKEN: "세션이 만료되었습니다. 다시 로그인해 주세요.",
  INVALID_RESET_TOKEN: "비밀번호 재설정 링크가 만료되었거나 잘못되었습니다.",
  QUEUE_TOKEN_NOT_FOUND:
    "대기열 정보가 만료되었습니다. 처음부터 다시 시도해 주세요.",
  QUEUE_TOKEN_EXPIRED: "대기열이 만료되었습니다. 다시 진입해 주세요.",
  QUEUE_TOKEN_MATCH_MISMATCH:
    "잘못된 대기열 접근입니다. 처음부터 다시 시도해 주세요.",
  SEAT_RESERVATION_EXPIRED:
    "좌석 점유 시간이 만료되었습니다. 처음부터 다시 시도해 주세요.",
  TOSS_API_ERROR:
    "결제 처리 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.",
};

/**
 * axios 에러에서 백엔드 응답의 { code, message } 추출
 * - axios 에러가 아니면 빈 객체 반환
 */
export function getErrorInfo(error: unknown): {
  code?: ErrorCode;
  message?: string;
} {
  if (!isAxiosError(error)) return {};
  const data = error.response?.data as
    | { code?: string; message?: string }
    | undefined;
  return {
    code: data?.code as ErrorCode | undefined,
    message: data?.message,
  };
}

/**
 * 화면 표시용 에러 메시지 결정
 * 우선순위: override 맵 → 백엔드 message → 호출자 fallback → 기본 문구
 */
export function resolveErrorMessage(
  error: unknown,
  fallback = "오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
): string {
  const { code, message } = getErrorInfo(error);
  if (code && ERROR_MESSAGE_OVERRIDES[code]) {
    return ERROR_MESSAGE_OVERRIDES[code]!;
  }
  return message ?? fallback;
}
