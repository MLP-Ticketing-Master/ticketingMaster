import { useQuery } from "@tanstack/react-query";
import { meApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_USER } from "@/lib/mock";

const useMock = true;

export const useMyProfile = () =>
  useQuery({
    queryKey: queryKeys.me.profile,
    queryFn: useMock ? async () => MOCK_USER : () => meApi.profile(),
  });

export const useMyStats = () =>
  useQuery({
    queryKey: queryKeys.me.stats,
    queryFn: useMock
      ? async () => ({
          totalBookings: 12,
          upcomingMatches: 3,
          watchedMatches: 5,
        })
      : () => meApi.stats(),
  });
