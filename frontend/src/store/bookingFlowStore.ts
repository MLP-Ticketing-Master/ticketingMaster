import { create } from "zustand";
import type { Seat } from "@/types";

export type BookingStep = "ZONE" | "SEAT" | "QUEUE" | "PAYMENT";

interface BookingFlowState {
  open: boolean;
  step: BookingStep;
  eventId: number | null;
  roundId: number | null;
  sectionId: number | null;
  selectedSeats: Seat[];
  /** 대기열 통과 후 입장 허용된 시각 (ms). null이면 아직 미입장. */
  admittedAt: number | null;
  openFlow: (params: { eventId: number; roundId: number }) => void;
  closeFlow: () => void;
  goToSeat: (sectionId: number) => void;
  goBackToZone: () => void;
  goToZone: () => void;
  goToQueue: () => void;
  goToPayment: () => void;
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
  admittedAt: null,
};

export const useBookingFlowStore = create<BookingFlowState>((set, get) => ({
  ...initial,
  openFlow: ({ eventId, roundId }) =>
    set({
      ...initial,
      open: true,
      step: "QUEUE",
      eventId,
      roundId,
    }),
  closeFlow: () => set({ ...initial }),
  goToSeat: (sectionId) => set({ step: "SEAT", sectionId }),
  goBackToZone: () => set({ step: "ZONE", sectionId: null }),
  // 대기열 통과 → admittedAt 기록
  goToZone: () =>
    set((state) => ({
      step: "ZONE",
      sectionId: null,
      admittedAt: state.admittedAt ?? Date.now(),
    })),
  goToQueue: () => set({ step: "QUEUE" }),
  goToPayment: () => set({ step: "PAYMENT" }),
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
