import { useQuery } from "@tanstack/react-query";
import { getAdminMatchDetail, getAdminMatches } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

export const useAdminMatches = (eventId?: number) =>
  useQuery({
    queryKey: queryKeys.admin.matches(eventId),
    queryFn: () => getAdminMatches({ eventId, page: 0, size: 100 }),
  });

export const useAdminMatchDetail = (matchId: number | null) =>
  useQuery({
    queryKey: queryKeys.admin.matchDetail(matchId ?? 0),
    queryFn: () => getAdminMatchDetail(matchId!),
    enabled: matchId !== null,
  });
