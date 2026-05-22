import api from "@/lib/axios";
import type { EventDetailResponse, EventListResponse, SportType } from "@/types";
import type { PageResponse } from "@/types/common";

export const eventApi = {
  /**
   * GET /events
   * 쿼리 파라미터: sportType?, status?, page, size, sort
   */
  list: (params?: {
    sportType?: Exclude<SportType, "ALL">;
    status?: string;
    page?: number;
    size?: number;
  }) =>
    api
      .get<PageResponse<EventListResponse>>("/events", { params })
      .then((r) => r.data),

  /**
   * GET /events/{eventId}
   */
  detail: (eventId: number) =>
    api.get<EventDetailResponse>(`/events/${eventId}`).then((r) => r.data),
};
