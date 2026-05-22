import type { SportType } from "@/types";

/** 백엔드 SportType → 한글 레이블 */
export const SPORT_LABEL: Record<Exclude<SportType, "ALL">, string> = {
  LOL: "리그 오브 레전드",
  VALORANT: "발로란트",
  OVERWATCH: "오버워치",
  TFT: "롤토체스",
  PUBG: "배틀그라운드",
  SC2: "스타크래프트2",
};

/** 하위 호환 alias */
export const GAME_LABEL = SPORT_LABEL;

export const SPORT_FILTER_LABEL: Record<SportType, string> = {
  ALL: "전체",
  LOL: "LOL",
  VALORANT: "발로란트",
  OVERWATCH: "오버워치",
  TFT: "TFT",
  PUBG: "배그",
  SC2: "스타",
};

export const GAME_FILTER_LABEL = SPORT_FILTER_LABEL;

export const SEAT_GRADE_COLORS: Record<string, string> = {
  VIP: "bg-violet-500",
  R: "bg-red-500",
  S: "bg-blue-500",
  A: "bg-green-500",
};

export const SEAT_GRADE_BG_SOFT: Record<string, string> = {
  VIP: "bg-violet-200",
  R: "bg-red-200",
  S: "bg-blue-200",
  A: "bg-green-200",
};

export const BRAND = {
  primary: "#FF6B47",
  primaryDark: "#ff5328",
  headerBg: "#2D2F3E",
} as const;
