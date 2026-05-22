import { create } from "zustand";
import type { SportType } from "@/types";

interface EventFilterState {
  sportType: SportType;
  keyword: string;
  setSportType: (sportType: SportType) => void;
  setKeyword: (keyword: string) => void;
  reset: () => void;
  // 하위 호환
  game: SportType;
  setGame: (game: SportType) => void;
}

export const useEventFilterStore = create<EventFilterState>((set) => ({
  sportType: "ALL",
  keyword: "",
  game: "ALL", // alias
  setSportType: (sportType) => set({ sportType, game: sportType }),
  setGame: (game) => set({ game, sportType: game }),
  setKeyword: (keyword) => set({ keyword }),
  reset: () => set({ sportType: "ALL", game: "ALL", keyword: "" }),
}));
