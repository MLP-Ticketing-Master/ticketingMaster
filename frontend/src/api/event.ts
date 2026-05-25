import api from "@/lib/axios";
import type {
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
