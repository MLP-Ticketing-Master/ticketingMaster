import { http } from "@/lib/axios";
import type { Round } from "@/types";

export const roundApi = {
  list: (eventId?: number) =>
    http
      .get<Round[]>("/rounds", {
        params: eventId ? { eventId } : {},
      })
      .then((r) => r.data),
  detail: (id: number) =>
    http.get<Round>(`/rounds/${id}`).then((r) => r.data),
};
