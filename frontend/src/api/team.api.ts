import api from "@/lib/axios";
import type { GameType, Team } from "@/types";

export const teamApi = {
  list: (game?: GameType) =>
    api
      .get<Team[]>("/teams", {
        params: game && game !== "ALL" ? { game } : {},
      })
      .then((r) => r.data),
  create: (body: Omit<Team, "id" | "registeredAt" | "totalMatches">) =>
    api.post<Team>("/admin/teams", body).then((r) => r.data),
  update: (id: number, body: Partial<Team>) =>
    api.put<Team>(`/admin/teams/${id}`, body).then((r) => r.data),
  remove: (id: number) =>
    api.delete(`/admin/teams/${id}`).then((r) => r.data),
};
