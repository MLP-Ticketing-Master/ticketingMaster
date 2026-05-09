import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_EVENTS, MOCK_EVENT_DETAIL } from "@/lib/mock";
import type { GameType } from "@/types";

const useMock = true;

export const useEventList = (game: GameType) =>
  useQuery({
    queryKey: queryKeys.events.list(game),
    queryFn: useMock
      ? async () =>
          game === "ALL"
            ? MOCK_EVENTS
            : MOCK_EVENTS.filter((e) => e.game === game)
      : () => eventApi.list(game),
  });

export const useEventDetail = (id: number) =>
  useQuery({
    queryKey: queryKeys.events.detail(id),
    queryFn: useMock
      ? async () => MOCK_EVENT_DETAIL
      : () => eventApi.detail(id),
    enabled: !!id,
  });
