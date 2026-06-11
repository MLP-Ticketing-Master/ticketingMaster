import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { getQueueStatus } from "@/api/queue";
import { queryKeys } from "@/lib/queryKeys";
import { useBookingFlowStore } from "@/store";

export const useQueueStatus = (
  matchId: number | null,
  queueToken: string | null,
) => {
  const setQueueStatus = useBookingFlowStore((s) => s.setQueueStatus);

  const query = useQuery({
    queryKey: queryKeys.queue.status(matchId ?? 0, queueToken),
    queryFn: () => getQueueStatus(matchId!, queueToken!),
    enabled: !!matchId && !!queueToken,
    refetchInterval: (q) =>
      q.state.data?.status === "ALLOWED" ? false : 3000,
  });

  useEffect(() => {
    if (query.data) setQueueStatus(query.data);
  }, [query.data, setQueueStatus]);

  return query;
};
