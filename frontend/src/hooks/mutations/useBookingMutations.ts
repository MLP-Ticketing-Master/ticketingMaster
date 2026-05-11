import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bookingApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type { BookingItem, CreateBookingRequest } from "@/types";

const useMock = true;

const mockCreate = async (body: CreateBookingRequest): Promise<BookingItem> => ({
  id: Date.now(),
  bookingNo: `B${Date.now()}`,
  eventTitle: "Mock Event",
  roundLabel: "Mock Round",
  startAt: new Date().toISOString(),
  venue: "Mock Venue",
  seatLabels: body.seatIds.map(String),
  amount: 0,
  status: "CONFIRMED",
  bookedAt: new Date().toISOString(),
});

export const useCreateBookingMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateBookingRequest) =>
      useMock ? mockCreate(body) : bookingApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.bookings.me }),
  });
};

export const useCancelBookingMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      if (useMock) return { id };
      return bookingApi.cancel(id);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.bookings.me }),
  });
};
