import type { SportType } from "@/types";

/** 백엔드 SportType → 한글 라벨 */
export const SPORT_LABEL: Record<Exclude<SportType, "ALL">, string> = {
  LOL: "리그 오브 레전드",
  VALORANT: "발로란트",
  OVERWATCH: "오버워치",
  TFT: "롤토체스",
  PUBG: "배틀그라운드",
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
};

export const GAME_FILTER_LABEL = SPORT_FILTER_LABEL;

export const BRAND = {
  primary: "#FF6B47",
  primaryDark: "#ff5328",
  headerBg: "#2D2F3E",
} as const;
