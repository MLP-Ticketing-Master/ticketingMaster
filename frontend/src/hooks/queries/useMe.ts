import { useQuery } from "@tanstack/react-query";
import { getProfile } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

export const useMyProfile = () =>
  useQuery({
    queryKey: queryKeys.me.profile,
    queryFn: () => getProfile(),
    staleTime: 1000 * 60 * 5, // 5분 캐시
  });
