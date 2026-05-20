import api from "@/lib/axios";
import type { EventDetail, EventSummary, GameType } from "@/types";

export const eventApi = {
  list: (game?: GameType) =>
    api
      .get<EventSummary[]>("/events", {
        params: game && game !== "ALL" ? { game } : {},
      })
      .then((r) => r.data),
  detail: (id: number) =>
    api.get<EventDetail>(`/events/${id}`).then((r) => r.data),
  search: (keyword: string) =>
    api
      .get<EventSummary[]>("/events/search", { params: { q: keyword } })
      .then((r) => r.data),
};
