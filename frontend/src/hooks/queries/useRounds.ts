import { useQuery } from "@tanstack/react-query";
import { roundApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_ROUNDS } from "@/lib/mock";

const useMock = true;

export const useRounds = (eventId?: number) =>
  useQuery({
    queryKey: queryKeys.rounds.list(eventId),
    queryFn: useMock
      ? async () =>
          eventId ? MOCK_ROUNDS.filter((r) => r.eventId === eventId) : MOCK_ROUNDS
      : () => roundApi.list(eventId),
  });
