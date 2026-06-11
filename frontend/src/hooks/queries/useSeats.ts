import { useQuery } from "@tanstack/react-query";
import { getMatchSections, getSectionSeats } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

/** GET /matches/{matchId}/sections — 구역 목록 + 등급별 잔여 */
export const useSeatSections = (matchId: number | null) =>
  useQuery({
    queryKey: queryKeys.matches.sections(matchId ?? 0),
    queryFn: () => getMatchSections(matchId!),
    enabled: !!matchId,
  });

/** GET /matches/{matchId}/sections/{sectionId}/seats — 구역 내 좌석 */
export const useSectionSeats = (matchId: number, sectionId?: number) =>
  useQuery({
    queryKey: queryKeys.matches.sectionSeats(matchId, sectionId ?? 0),
    queryFn: () => getSectionSeats(matchId, sectionId!),
    enabled: !!matchId && !!sectionId,
  });
