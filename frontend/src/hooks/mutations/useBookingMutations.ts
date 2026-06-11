import { useMutation, useQueryClient } from "@tanstack/react-query";
import { cancelBooking, createBooking } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type { CreateBookingRequest } from "@/types";

export const useCreateBookingMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateBookingRequest) => createBooking(body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.bookings.me }),
  });
};

export const useCancelBookingMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => cancelBooking(id),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.bookings.me }),
  });
};
