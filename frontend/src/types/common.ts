export type GameType =
  | "LOL"
  | "VALORANT"
  | "OVERWATCH"
  | "TFT"
  | "PUBG"
  | "SC2"
  | "ALL";

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

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ApiError {
  code: string;
  message: string;
}
