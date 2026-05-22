import api from "@/lib/axios";
import type { GameType, Team } from "@/types";

/** GET /teams */
export async function getTeamList(game?: GameType): Promise<Team[]> {
  const res = await api.get<Team[]>("/teams", {
    params: game && game !== "ALL" ? { game } : {},
  });
  return res.data;
}

/** POST /admin/teams */
export async function createTeam(
  body: Omit<Team, "id" | "registeredAt" | "totalMatches">,
): Promise<Team> {
  const res = await api.post<Team>("/admin/teams", body);
  return res.data;
}

/** PUT /admin/teams/{id} */
export async function updateTeam(
  id: number,
  body: Partial<Team>,
): Promise<Team> {
  const res = await api.put<Team>(`/admin/teams/${id}`, body);
  return res.data;
}

/** DELETE /admin/teams/{id} */
export async function deleteTeam(id: number): Promise<unknown> {
  const res = await api.delete(`/admin/teams/${id}`);
  return res.data;
}
