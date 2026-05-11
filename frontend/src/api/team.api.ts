import { http } from "@/lib/axios";
import type { GameType, Team } from "@/types";

export const teamApi = {
  list: (game?: GameType) =>
    http
      .get<Team[]>("/teams", {
        params: game && game !== "ALL" ? { game } : {},
      })
      .then((r) => r.data),
  create: (body: Omit<Team, "id" | "registeredAt" | "totalMatches">) =>
    http.post<Team>("/admin/teams", body).then((r) => r.data),
  update: (id: number, body: Partial<Team>) =>
    http.put<Team>(`/admin/teams/${id}`, body).then((r) => r.data),
  remove: (id: number) =>
    http.delete(`/admin/teams/${id}`).then((r) => r.data),
};
