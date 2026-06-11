import type { SportType } from "./common";

/** 백엔드 SportType — "ALL"은 프론트 필터 전용이므로 팀 데이터에는 사용 안 함 */
export type TeamSportType = Exclude<SportType, "ALL">;

/** GET /admin/teams 응답 (백엔드 AdminTeamResponse) */
export interface Team {
  teamId: number;
  name: string;
  sportType: TeamSportType;
  logoImageUrl: string | null;
}

/** POST /admin/teams 요청 */
export interface CreateTeamRequest {
  name: string;
  sportType: TeamSportType;
  logoImageUrl?: string;
}

/** PATCH /admin/teams/{teamId} 요청 (부분 수정) */
export interface UpdateTeamRequest {
  name?: string;
  sportType?: TeamSportType;
  logoImageUrl?: string;
}
