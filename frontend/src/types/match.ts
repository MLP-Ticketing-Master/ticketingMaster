// 백엔드 MatchStatus enum 과 일치 (admin 전용)
export type AdminMatchStatus = "SCHEDULED" | "LIVE" | "FINISHED" | "CANCELED";

/** GET /admin/matches 목록 / GET /admin/matches/{id} 상세 응답 */
export interface AdminMatchResponse {
  id: number;
  eventId: number;
  roundLabel: string;
  homeTeamId: number | null;
  awayTeamId: number | null;
  matchDate: string;              // "YYYY-MM-DD"
  startAt: string;                // ISO LocalDateTime
  endAt: string;
  bookingOpenAt: string;
  bookingCloseAt: string;
  cancelAvailableUntil: string | null;
  status: AdminMatchStatus;
  deletedAt: string | null;
}

/** POST /admin/events/{eventId}/matches 요청 */
export interface AdminMatchCreateRequest {
  roundLabel: string;
  matchDate: string;              // "YYYY-MM-DD"
  startAt: string;                // ISO LocalDateTime ("YYYY-MM-DDTHH:mm:ss")
  endAt?: string;                 // 미지정 시 백엔드가 startAt+2h
  bookingOpenAt: string;
  bookingCloseAt: string;
  cancelAvailableUntil?: string;
  homeTeamId?: number;
  awayTeamId?: number;
}

/** PATCH /admin/matches/{matchId} 요청 (부분 수정) */
export interface AdminMatchUpdateRequest {
  roundLabel?: string;
  homeTeamId?: number;
  awayTeamId?: number;
  matchDate?: string;
  startAt?: string;
  endAt?: string;
  bookingOpenAt?: string;
  bookingCloseAt?: string;
  cancelAvailableUntil?: string;
  status?: AdminMatchStatus;
}
