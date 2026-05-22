export type SportType =
  | "LOL"
  | "VALORANT"
  | "OVERWATCH"
  | "TFT"
  | "PUBG"
  | "SC2"
  | "ALL"; // 프론트 필터 전용

export type GameType = SportType; // 하위 호환 유지

export type EventStatus = "SCHEDULED" | "ON_SALE" | "CLOSED" | "CANCELED";

export type BookingStatus =
  | "CONFIRMED"
  | "CANCELED"
  | "PENDING_PAYMENT"
  | "WATCHED";

export type SeatGradeCode = "VIP" | "R" | "S" | "A";

export type SeatStatus = "AVAILABLE" | "SOLD" | "HOLD" | "DISABLED";

export interface Pageable {
  page: number;
  size: number;
}

/** Spring Page 응답 구조 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // 현재 페이지 (0-based)
  size: number;
}

export interface ApiError {
  code: string;
  message: string;
}
