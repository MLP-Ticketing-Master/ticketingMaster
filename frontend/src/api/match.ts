import api from "@/lib/axios";
import type {
  AdminMatchCreateRequest,
  AdminMatchResponse,
  AdminMatchUpdateRequest,
} from "@/types";
import type { PageResponse } from "@/types/common";

/** GET /admin/matches?eventId= (Spring Pageable) */
export async function getAdminMatches(params?: {
  eventId?: number;
  page?: number;
  size?: number;
}): Promise<PageResponse<AdminMatchResponse>> {
  const res = await api.get<PageResponse<AdminMatchResponse>>(
    "/admin/matches",
    { params },
  );
  return res.data;
}

/** GET /admin/matches/{matchId} */
export async function getAdminMatchDetail(
  matchId: number,
): Promise<AdminMatchResponse> {
  const res = await api.get<AdminMatchResponse>(`/admin/matches/${matchId}`);
  return res.data;
}

/** POST /admin/events/{eventId}/matches */
export async function createMatch(
  eventId: number,
  body: AdminMatchCreateRequest,
): Promise<AdminMatchResponse> {
  const res = await api.post<AdminMatchResponse>(
    `/admin/events/${eventId}/matches`,
    body,
  );
  return res.data;
}

/** PATCH /admin/matches/{matchId} */
export async function updateMatch(
  matchId: number,
  body: AdminMatchUpdateRequest,
): Promise<AdminMatchResponse> {
  const res = await api.patch<AdminMatchResponse>(
    `/admin/matches/${matchId}`,
    body,
  );
  return res.data;
}

/** DELETE /admin/matches/{matchId} */
export async function deleteMatch(matchId: number): Promise<void> {
  await api.delete(`/admin/matches/${matchId}`);
}
