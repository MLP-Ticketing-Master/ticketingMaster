import { useQuery } from "@tanstack/react-query";
import { getEventDetail, getEventList } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type { SportType } from "@/types";

export const useEventList = (
  sportType: SportType = "ALL",
  page = 0,
  size = 20,
) =>
  useQuery({
    queryKey: queryKeys.events.list(sportType),
    queryFn: async () => {
      const pageData = await getEventList({
        ...(sportType !== "ALL" ? { sportType } : {}),
        page,
        size,
      });
      return pageData.content;
    },
  });

export const useEventDetail = (id: number) =>
  useQuery({
    queryKey: queryKeys.events.detail(id),
    queryFn: () => getEventDetail(id),
    enabled: !!id,
  });
