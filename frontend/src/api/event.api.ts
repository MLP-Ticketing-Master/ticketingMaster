import { http } from "@/lib/axios";
import type { EventDetail, EventSummary, GameType } from "@/types";

export const eventApi = {
  list: (game?: GameType) =>
    http
      .get<EventSummary[]>("/events", {
        params: game && game !== "ALL" ? { game } : {},
      })
      .then((r) => r.data),
  detail: (id: number) =>
    http.get<EventDetail>(`/events/${id}`).then((r) => r.data),
  search: (keyword: string) =>
    http
      .get<EventSummary[]>("/events/search", { params: { q: keyword } })
      .then((r) => r.data),
};
