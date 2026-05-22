import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type { CreateBookingRequest } from "@/types";

export const useCreateBookingMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateBookingRequest) => bookingApi.create(body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.bookings.me }),
  });
};

export const useCancelBookingMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    // 취소 사유는 선택 사항 — 백엔드 @RequestBody cancelReason 전송
    mutationFn: (id: number) => bookingApi.cancel(id),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.bookings.me }),
  });
};
