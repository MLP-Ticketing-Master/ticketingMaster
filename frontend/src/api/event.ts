import api from "@/lib/axios";
import type {
  AdminEventCreateRequest,
  AdminEventDetailResponse,
  AdminEventListResponse,
  AdminEventResponse,
  AdminEventUpdateRequest,
  EventDetailResponse,
  EventListResponse,
  SportType,
} from "@/types";
import type { PageResponse } from "@/types/common";

/**
 * GET /events
 * 쿼리 파라미터: sportType?, status?, page, size, sort
 */
export async function getEventList(params?: {
  sportType?: Exclude<SportType, "ALL">;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<EventListResponse>> {
  const res = await api.get<PageResponse<EventListResponse>>("/events", {
    params,
  });
  return res.data;
}

/** GET /events/{eventId} */
export async function getEventDetail(
  eventId: number,
): Promise<EventDetailResponse> {
  const res = await api.get<EventDetailResponse>(`/events/${eventId}`);
  return res.data;
}

// ── Admin ────────────────────────────────────────────────────────

/** GET /admin/events (Spring Pageable 페이지네이션) */
export async function getAdminEvents(params?: {
  page?: number;
  size?: number;
}): Promise<PageResponse<AdminEventListResponse>> {
  const res = await api.get<PageResponse<AdminEventListResponse>>(
    "/admin/events",
    { params },
  );
  return res.data;
}

/** GET /admin/events/{eventId} */
export async function getAdminEventDetail(
  eventId: number,
): Promise<AdminEventDetailResponse> {
  const res = await api.get<AdminEventDetailResponse>(
    `/admin/events/${eventId}`,
  );
  return res.data;
}

/** POST /admin/events */
export async function createEvent(
  body: AdminEventCreateRequest,
): Promise<AdminEventResponse> {
  const res = await api.post<AdminEventResponse>("/admin/events", body);
  return res.data;
}

/** PATCH /admin/events/{eventId} */
export async function updateEvent(
  eventId: number,
  body: AdminEventUpdateRequest,
): Promise<AdminEventResponse> {
  const res = await api.patch<AdminEventResponse>(
    `/admin/events/${eventId}`,
    body,
  );
  return res.data;
}

/** DELETE /admin/events/{eventId} */
export async function deleteEvent(eventId: number): Promise<void> {
  await api.delete(`/admin/events/${eventId}`);
}
