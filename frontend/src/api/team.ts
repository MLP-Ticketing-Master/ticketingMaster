import api from "@/lib/axios";
import type {
  CreateTeamRequest,
  Team,
  TeamSportType,
  UpdateTeamRequest,
} from "@/types";

/** GET /admin/teams (sportType 필터 옵셔널) */
export async function getAdminTeams(
  sportType?: TeamSportType,
): Promise<Team[]> {
  const res = await api.get<Team[]>("/admin/teams", {
    params: sportType ? { sportType } : {},
  });
  return res.data;
}

/** POST /admin/teams */
export async function createTeam(body: CreateTeamRequest): Promise<Team> {
  const res = await api.post<Team>("/admin/teams", body);
  return res.data;
}

/** PATCH /admin/teams/{teamId} */
export async function updateTeam(
  teamId: number,
  body: UpdateTeamRequest,
): Promise<Team> {
  const res = await api.patch<Team>(`/admin/teams/${teamId}`, body);
  return res.data;
}

/** DELETE /admin/teams/{teamId} */
export async function deleteTeam(teamId: number): Promise<void> {
  await api.delete(`/admin/teams/${teamId}`);
}
