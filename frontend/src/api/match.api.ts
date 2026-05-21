import api from "@/lib/axios";
import type { Match } from "@/types";

export const matchApi = {
  list: (eventId?: number) =>
    api
      .get<Match[]>("/matches", {
        params: eventId ? { eventId } : {},
      })
      .then((r) => r.data),
  detail: (id: number) =>
    api.get<Match>(`/matches/${id}`).then((r) => r.data),
};
