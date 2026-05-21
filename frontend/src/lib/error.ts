/**
 * 백엔드 API 응답의 error code 상수
 * 도메인 작업 시 해당 도메인 에러 코드를 여기에 추가
 */
export const ERROR_CODES = {
  // 공통
  UNAUTHORIZED: "UNAUTHORIZED",
  FORBIDDEN: "FORBIDDEN",
  INVALID_INPUT: "INVALID_INPUT",
  USER_NOT_FOUND: "USER_NOT_FOUND",

  // Match
  MATCH_NOT_FOUND: "MATCH_NOT_FOUND",
  BOOKING_NOT_OPEN: "BOOKING_NOT_OPEN",

  // Queue
  QUEUE_TOKEN_NOT_FOUND: "QUEUE_TOKEN_NOT_FOUND",
  QUEUE_TOKEN_EXPIRED: "QUEUE_TOKEN_EXPIRED",
  QUEUE_TOKEN_MATCH_MISMATCH: "QUEUE_TOKEN_MATCH_MISMATCH",
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
