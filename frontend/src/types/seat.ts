import type { SeatStatus } from "./common";

// 백엔드 SeatItem 매핑
export interface Seat {
  seatId: number;
  seatCode: string;
  rowLabel: string;
  seatNo: number;
  gradeCode: string;
  colorHex: string;
  price: number;
  status: SeatStatus;
}

// GET /matches/{matchId}/sections/{sectionId}/seats 응답
export interface SectionSeatList {
  matchId: number;
  sectionId: number;
  sectionName: string;
  seats: Seat[];
}

// POST /matches/{matchId}/seats/reserve 응답
export interface SeatReserveResult {
  reservedSeatIds: number[];
  reservedUntil: string;
  totalPrice: number;
}

// DELETE /matches/{matchId}/seats/reserve 응답
export interface SeatReleaseResult {
  releasedSeatIds: number[];
}
