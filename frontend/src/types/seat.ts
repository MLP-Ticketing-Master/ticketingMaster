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
  /** 프론트 표시용 — 좌석 선택 시점에 SectionSeatList.sectionName 붙임 */
  sectionName?: string;
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

// ── Admin: 좌석 등급 ────────────────────────────────────────────

/** GET/POST/PATCH 응답 — 좌석 등급 */
export interface AdminSeatGradeResponse {
  seatGradeId: number;
  gradeCode: string;
  price: number;
  colorHex: string | null;
}

/** POST /admin/events/{eventId}/seat-grades 요청 */
export interface AdminSeatGradeCreateRequest {
  gradeCode: string;
  price: number;
  colorHex?: string;
}

/** PATCH /admin/seat-grades/{seatGradeId} 요청 (부분 수정) */
export interface AdminSeatGradeUpdateRequest {
  price?: number;
  colorHex?: string;
}

// ── Admin: 구역 ─────────────────────────────────────────────────

/** GET/POST/PATCH 응답 — 구역 */
export interface AdminSectionResponse {
  sectionId: number;
  name: string;
  displayOrder: number;
  description: string | null;
}

/** POST /admin/events/{eventId}/sections 요청 */
export interface AdminSectionCreateRequest {
  name: string;
  displayOrder: number;
  description?: string;
}

/** PATCH /admin/sections/{sectionId} 요청 (부분 수정) */
export interface AdminSectionUpdateRequest {
  name?: string;
  displayOrder?: number;
  description?: string;
}

// ── Admin: 좌석 ─────────────────────────────────────────────────

/** GET /admin/matches/{matchId}/seats 응답 요소 */
export interface AdminSeatResponse {
  seatId: number;
  seatCode: string;
  sectionName: string;
  gradeCode: string;
  price: number;
  status: SeatStatus;
}

/** POST /admin/matches/{matchId}/seats 요청 단건 (bulk 요소이기도 함) */
export interface AdminSeatCreateRequest {
  sectionId: number;
  seatGradeId: number;
  rowLabel: string;
  seatNo: number;
}

/** POST /admin/matches/{matchId}/seats/bulk 요청 */
export interface AdminSeatBulkCreateRequest {
  seats: AdminSeatCreateRequest[];
}

/** PATCH /admin/seats/{seatId} 요청 (구역/등급 변경) */
export interface AdminSeatUpdateRequest {
  sectionId?: number;
  seatGradeId?: number;
}
