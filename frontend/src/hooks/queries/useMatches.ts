import { useQuery } from "@tanstack/react-query";
import { matchApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_MATCHES } from "@/lib/mock";

const useMock = true;

export const useMatches = (eventId?: number) =>
  useQuery({
    queryKey: queryKeys.matches.list(eventId),
    queryFn: useMock
      ? async () =>
          eventId ? MOCK_MATCHES.filter((m) => m.eventId === eventId) : MOCK_MATCHES
      : () => matchApi.list(eventId),
  });
