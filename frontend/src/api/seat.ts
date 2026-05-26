import api from "@/lib/axios";
import type {
  AdminSeatBulkCreateRequest,
  AdminSeatGradeCreateRequest,
  AdminSeatGradeResponse,
  AdminSeatGradeUpdateRequest,
  AdminSeatResponse,
  AdminSeatUpdateRequest,
  AdminSectionCreateRequest,
  AdminSectionResponse,
  AdminSectionUpdateRequest,
  SeatSectionListResponse,
  SectionSeatList,
  SeatReleaseResult,
  SeatReserveResult,
} from "@/types";

/**
 * 좌석 선택 1단계 — 구역 목록 + 등급별 잔여
 * GET /matches/{matchId}/sections
 */
export async function getMatchSections(
  matchId: number,
): Promise<SeatSectionListResponse> {
  const res = await api.get<SeatSectionListResponse>(
    `/matches/${matchId}/sections`,
  );
  return res.data;
}

/**
 * 좌석 선택 2단계 — 구역 내 좌석 목록
 * GET /matches/{matchId}/sections/{sectionId}/seats
 */
export async function getSectionSeats(
  matchId: number,
  sectionId: number,
): Promise<SectionSeatList> {
  const res = await api.get<SectionSeatList>(
    `/matches/${matchId}/sections/${sectionId}/seats`,
  );
  return res.data;
}

/**
 * 좌석 일괄 점유 (USER 인증 필요)
 * POST /matches/{matchId}/seats/reserve
 */
export async function reserveSeats(
  matchId: number,
  seatIds: number[],
): Promise<SeatReserveResult> {
  const res = await api.post<SeatReserveResult>(
    `/matches/${matchId}/seats/reserve`,
    { seatIds },
  );
  return res.data;
}

/**
 * 본인 점유 좌석 해제 (USER 인증 필요)
 * DELETE /matches/{matchId}/seats/reserve
 */
export async function releaseSeats(
  matchId: number,
  seatIds: number[],
): Promise<SeatReleaseResult> {
  const res = await api.delete<SeatReleaseResult>(
    `/matches/${matchId}/seats/reserve`,
    { data: { seatIds } },
  );
  return res.data;
}

// ── Admin: 좌석 등급 ────────────────────────────────────────────

/** GET /admin/events/{eventId}/seat-grades */
export async function getAdminSeatGrades(
  eventId: number,
): Promise<AdminSeatGradeResponse[]> {
  const res = await api.get<AdminSeatGradeResponse[]>(
    `/admin/events/${eventId}/seat-grades`,
  );
  return res.data;
}

/** POST /admin/events/{eventId}/seat-grades */
export async function createAdminSeatGrade(
  eventId: number,
  body: AdminSeatGradeCreateRequest,
): Promise<AdminSeatGradeResponse> {
  const res = await api.post<AdminSeatGradeResponse>(
    `/admin/events/${eventId}/seat-grades`,
    body,
  );
  return res.data;
}

/** PATCH /admin/seat-grades/{seatGradeId} */
export async function updateAdminSeatGrade(
  seatGradeId: number,
  body: AdminSeatGradeUpdateRequest,
): Promise<AdminSeatGradeResponse> {
  const res = await api.patch<AdminSeatGradeResponse>(
    `/admin/seat-grades/${seatGradeId}`,
    body,
  );
  return res.data;
}

/** DELETE /admin/seat-grades/{seatGradeId} */
export async function deleteAdminSeatGrade(
  seatGradeId: number,
): Promise<void> {
  await api.delete(`/admin/seat-grades/${seatGradeId}`);
}

// ── Admin: 구역 ─────────────────────────────────────────────────

/** GET /admin/events/{eventId}/sections */
export async function getAdminSections(
  eventId: number,
): Promise<AdminSectionResponse[]> {
  const res = await api.get<AdminSectionResponse[]>(
    `/admin/events/${eventId}/sections`,
  );
  return res.data;
}

/** POST /admin/events/{eventId}/sections */
export async function createAdminSection(
  eventId: number,
  body: AdminSectionCreateRequest,
): Promise<AdminSectionResponse> {
  const res = await api.post<AdminSectionResponse>(
    `/admin/events/${eventId}/sections`,
    body,
  );
  return res.data;
}

/** PATCH /admin/sections/{sectionId} */
export async function updateAdminSection(
  sectionId: number,
  body: AdminSectionUpdateRequest,
): Promise<AdminSectionResponse> {
  const res = await api.patch<AdminSectionResponse>(
    `/admin/sections/${sectionId}`,
    body,
  );
  return res.data;
}

/** DELETE /admin/sections/{sectionId} */
export async function deleteAdminSection(sectionId: number): Promise<void> {
  await api.delete(`/admin/sections/${sectionId}`);
}

// ── Admin: 좌석 ─────────────────────────────────────────────────

/** GET /admin/matches/{matchId}/seats */
export async function getAdminSeats(
  matchId: number,
): Promise<AdminSeatResponse[]> {
  const res = await api.get<AdminSeatResponse[]>(
    `/admin/matches/${matchId}/seats`,
  );
  return res.data;
}

/** POST /admin/matches/{matchId}/seats/bulk */
export async function bulkCreateAdminSeats(
  matchId: number,
  body: AdminSeatBulkCreateRequest,
): Promise<AdminSeatResponse[]> {
  const res = await api.post<AdminSeatResponse[]>(
    `/admin/matches/${matchId}/seats/bulk`,
    body,
  );
  return res.data;
}

/** PATCH /admin/seats/{seatId} */
export async function updateAdminSeat(
  seatId: number,
  body: AdminSeatUpdateRequest,
): Promise<AdminSeatResponse> {
  const res = await api.patch<AdminSeatResponse>(
    `/admin/seats/${seatId}`,
    body,
  );
  return res.data;
}

/** DELETE /admin/seats/{seatId} */
export async function deleteAdminSeat(seatId: number): Promise<void> {
  await api.delete(`/admin/seats/${seatId}`);
}
