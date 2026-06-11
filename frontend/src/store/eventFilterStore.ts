import { create } from "zustand";
import type { SportType, EventStatus } from "@/types";

export interface EventFilterState {
  // 종목 필터
  sportType: SportType;
  // 진행 상태 필터 (빈 배열 = 전체)
  statuses: EventStatus[];
  // 날짜 범위 필터 (null = 제한 없음)
  dateFrom: string | null; // "YYYY-MM-DD"
  dateTo: string | null;   // "YYYY-MM-DD"

  // actions
  setSportType: (sportType: SportType) => void;
  toggleStatus: (status: EventStatus) => void;
  setDateRange: (from: string | null, to: string | null) => void;
  reset: () => void;

  // 필터 적용 여부 (아이콘 색상용)
  hasActiveFilters: boolean;

  // 하위 호환
  game: SportType;
  setGame: (game: SportType) => void;
  keyword: string;
  setKeyword: (keyword: string) => void;
}

const DEFAULT_STATE = {
  sportType: "ALL" as SportType,
  game: "ALL" as SportType,
  statuses: [] as EventStatus[],
  dateFrom: null as string | null,
  dateTo: null as string | null,
  keyword: "",
};

function computeHasActiveFilters(
  sportType: SportType,
  statuses: EventStatus[],
  dateFrom: string | null,
  dateTo: string | null,
) {
  return sportType !== "ALL" || statuses.length > 0 || dateFrom !== null || dateTo !== null;
}

export const useEventFilterStore = create<EventFilterState>((set) => ({
  ...DEFAULT_STATE,
  hasActiveFilters: false,

  setSportType: (sportType) =>
    set((s) => ({
      sportType,
      game: sportType,
      hasActiveFilters: computeHasActiveFilters(sportType, s.statuses, s.dateFrom, s.dateTo),
    })),

  setGame: (game) =>
    set((s) => ({
      game,
      sportType: game,
      hasActiveFilters: computeHasActiveFilters(game, s.statuses, s.dateFrom, s.dateTo),
    })),

  toggleStatus: (status) =>
    set((s) => {
      const next = s.statuses.includes(status)
        ? s.statuses.filter((st) => st !== status)
        : [...s.statuses, status];
      return {
        statuses: next,
        hasActiveFilters: computeHasActiveFilters(s.sportType, next, s.dateFrom, s.dateTo),
      };
    }),

  setDateRange: (dateFrom, dateTo) =>
    set((s) => ({
      dateFrom,
      dateTo,
      hasActiveFilters: computeHasActiveFilters(s.sportType, s.statuses, dateFrom, dateTo),
    })),

  setKeyword: (keyword) => set({ keyword }),

  reset: () =>
    set({
      ...DEFAULT_STATE,
      hasActiveFilters: false,
    }),
}));
