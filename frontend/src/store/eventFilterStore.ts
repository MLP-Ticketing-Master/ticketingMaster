import { create } from "zustand";
import type { GameType } from "@/types";

interface EventFilterState {
  game: GameType;
  keyword: string;
  setGame: (game: GameType) => void;
  setKeyword: (keyword: string) => void;
  reset: () => void;
}

export const useEventFilterStore = create<EventFilterState>((set) => ({
  game: "ALL",
  keyword: "",
  setGame: (game) => set({ game }),
  setKeyword: (keyword) => set({ keyword }),
  reset: () => set({ game: "ALL", keyword: "" }),
}));
