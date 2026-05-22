import type { GameType } from "./common";

export type MatchStatus = "SCHEDULED" | "LIVE" | "FINISHED";

// 리그 코드: LCK·LPL·LEC·LCS·MSI·WORLDS·VCT·OWCS 등
export type LeagueCode =
  | "LCK"
  | "LPL"
  | "LEC"
  | "LCS"
  | "MSI"
  | "WORLDS"
  | "VCT"
  | "OWCS"
  | "PCS";

export interface ScheduleTeam {
  code: string; // 약어 (예: "T1")
  name: string; // 풀네임 (예: "T1")
  // 팀 컬러 — Tailwind bg-* 클래스 사용
  color: string;
}

export interface ScheduledMatch {
  id: number;
  leagueCode: LeagueCode;
  leagueName: string;
  game: GameType;
  teamA: ScheduleTeam;
  teamB: ScheduleTeam;
  startAt: string; // ISO datetime
  status: MatchStatus;
  bestOf: number; // 1, 3, 5
  scoreA?: number;
  scoreB?: number;
  venue?: string;
}
