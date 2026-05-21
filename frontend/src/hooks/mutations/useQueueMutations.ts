import { useMutation } from "@tanstack/react-query";
import { enterQueue } from "@/api/queue.api";
import { useBookingFlowStore } from "@/store";

export const useEnterQueueMutation = () => {
  const setQueueEnterResult = useBookingFlowStore((s) => s.setQueueEnterResult);

  return useMutation({
    mutationFn: (matchId: number) => enterQueue(matchId),
    onSuccess: (data) => setQueueEnterResult(data),
  });
};
