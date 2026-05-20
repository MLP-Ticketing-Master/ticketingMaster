import api from "@/lib/axios";
import type { Round } from "@/types";

export const roundApi = {
  list: (eventId?: number) =>
    api
      .get<Round[]>("/rounds", {
        params: eventId ? { eventId } : {},
      })
      .then((r) => r.data),
  detail: (id: number) =>
    api.get<Round>(`/rounds/${id}`).then((r) => r.data),
};
