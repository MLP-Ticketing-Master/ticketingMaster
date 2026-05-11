import { create } from "zustand";
import type { Seat } from "@/types";

export type BookingStep = "ZONE" | "SEAT";

interface BookingFlowState {
  open: boolean;
  step: BookingStep;
  eventId: number | null;
  roundId: number | null;
  sectionId: number | null;
  selectedSeats: Seat[];
  openFlow: (params: { eventId: number; roundId: number }) => void;
  closeFlow: () => void;
  goToSeat: (sectionId: number) => void;
  goBackToZone: () => void;
  toggleSeat: (seat: Seat) => void;
  removeSeat: (seatId: number) => void;
  reset: () => void;
}

const initial = {
  open: false,
  step: "ZONE" as BookingStep,
  eventId: null,
  roundId: null,
  sectionId: null,
  selectedSeats: [],
};

export const useBookingFlowStore = create<BookingFlowState>((set, get) => ({
  ...initial,
  openFlow: ({ eventId, roundId }) =>
    set({
      ...initial,
      open: true,
      eventId,
      roundId,
    }),
  closeFlow: () => set({ ...initial }),
  goToSeat: (sectionId) => set({ step: "SEAT", sectionId }),
  goBackToZone: () => set({ step: "ZONE", sectionId: null }),
  toggleSeat: (seat) => {
    const exists = get().selectedSeats.find((s) => s.id === seat.id);
    set({
      selectedSeats: exists
        ? get().selectedSeats.filter((s) => s.id !== seat.id)
        : [...get().selectedSeats, seat],
    });
  },
  removeSeat: (seatId) =>
    set({
      selectedSeats: get().selectedSeats.filter((s) => s.id !== seatId),
    }),
  reset: () => set({ ...initial }),
}));
