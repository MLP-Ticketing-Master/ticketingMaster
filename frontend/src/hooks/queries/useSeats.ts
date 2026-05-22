import { useQuery } from "@tanstack/react-query";
import { seatApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

/**
 * 구역 목록 + 등급별 잔여 — GET /matches/{matchId}/sections
 * 이벤트 상세 페이지에서 회차 선택 시 호출
 */
export const useSeatSections = (matchId: number | null) =>
  useQuery({
    queryKey: ["seat-sections", matchId],
    queryFn: () => seatApi.sections(matchId!),
    enabled: !!matchId,
  });

/** @deprecated mock 전용 — 실 API 없음 */
export const useSeatGrades = (eventId: number) =>
  useQuery({
    queryKey: ["seat-grades", eventId],
    queryFn: () => Promise.resolve([]),
    enabled: false,
  });

/** @deprecated mock 전용 */
export const useSections = (eventId: number) =>
  useQuery({
    queryKey: ["sections", eventId],
    queryFn: () => Promise.resolve([]),
    enabled: false,
  });

export const useSeatLayout = (matchId: number, sectionId?: number) =>
  useQuery({
    queryKey: queryKeys.matches.seatLayout(matchId),
    queryFn: () => seatApi.sectionSeats(matchId, sectionId!),
    enabled: !!matchId && !!sectionId,
  });
