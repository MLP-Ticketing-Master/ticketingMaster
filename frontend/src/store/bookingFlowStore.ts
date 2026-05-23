import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";
import type { QueueEnterResponse, QueueStatusResponse, Seat } from "@/types";

export type BookingStep = "ZONE" | "SEAT" | "QUEUE" | "PAYMENT";

interface BookingFlowState {
  open: boolean;
  step: BookingStep;
  eventId: number | null;
  matchId: number | null;
  sectionId: number | null;
  selectedSeats: Seat[];

  // 대기열 상태
  queueToken: string | null;
  queueNumber: number | null;
  remainingAhead: number | null;
  estimatedWaitSeconds: number | null;
  allowedAt: string | null;
  entryDeadline: string | null;

  // 좌석 점유 만료 시각 — reserve 응답의 reservedUntil 저장
  reservedUntil: string | null;

  openFlow: (params: { eventId: number; matchId: number }) => void;
  closeFlow: () => void;
  goToSeat: (sectionId: number) => void;
  goBackToZone: () => void;
  goBackToSeatStep: () => void;
  goToZone: () => void;
  goToQueue: () => void;
  goToPayment: () => void;
  toggleSeat: (seat: Seat) => void;
  removeSeat: (seatId: number) => void;
  reset: () => void;

  setQueueEnterResult: (res: QueueEnterResponse) => void;
  setQueueStatus: (res: QueueStatusResponse) => void;
  clearQueue: () => void;

  // 좌석 점유 만료 시각 setter
  setReservedUntil: (until: string | null) => void;
}

const initial = {
  open: false,
  step: "ZONE" as BookingStep,
  eventId: null,
  matchId: null,
  sectionId: null,
  selectedSeats: [],
  queueToken: null,
  queueNumber: null,
  remainingAhead: null,
  estimatedWaitSeconds: null,
  allowedAt: null,
  entryDeadline: null,
  reservedUntil: null,
};

export const useBookingFlowStore = create<BookingFlowState>()(
  persist(
    (set, get) => ({
      ...initial,
      openFlow: ({ eventId, matchId }) => {
        // 다른 매치로 들어오면 이전 큐 상태 초기화. 같은 매치면 토큰/순번 유지
        const prev = get();
        const sameMatch = prev.matchId === matchId && !!prev.queueToken;
        set({
          ...initial,
          open: true,
          step: "QUEUE",
          eventId,
          matchId,
          ...(sameMatch
            ? {
                queueToken: prev.queueToken,
                queueNumber: prev.queueNumber,
                remainingAhead: prev.remainingAhead,
                estimatedWaitSeconds: prev.estimatedWaitSeconds,
              }
            : {}),
        });
      },
      closeFlow: () => set({ ...initial }),
      goToSeat: (sectionId) => set({ step: "SEAT", sectionId }),
      goBackToZone: () => set({ step: "ZONE", sectionId: null }),
      // 결제 단계에서 좌석 다시 선택 — 선택 좌석 비우고 SEAT 단계로 복귀
      goBackToSeatStep: () => set({ step: "SEAT", selectedSeats: [] }),
      // 대기열 통과 → 좌석 선택 단계 진입
      goToZone: () => set({ step: "ZONE", sectionId: null }),
      goToQueue: () => set({ step: "QUEUE" }),
      goToPayment: () => set({ step: "PAYMENT" }),
      toggleSeat: (seat) => {
        const exists = get().selectedSeats.find(
          (s) => s.seatId === seat.seatId,
        );
        set({
          selectedSeats: exists
            ? get().selectedSeats.filter((s) => s.seatId !== seat.seatId)
            : [...get().selectedSeats, seat],
        });
      },
      removeSeat: (seatId) =>
        set({
          selectedSeats: get().selectedSeats.filter(
            (s) => s.seatId !== seatId,
          ),
        }),
      reset: () => set({ ...initial }),

      setQueueEnterResult: (res) =>
        set({
          queueToken: res.queueToken,
          queueNumber: res.queueNumber,
          remainingAhead: res.remainingAhead,
          estimatedWaitSeconds: res.estimatedWaitSeconds,
          allowedAt: res.allowedAt,
          entryDeadline: res.entryDeadline,
        }),
      setQueueStatus: (res) =>
        set({
          queueNumber: res.queueNumber,
          remainingAhead: res.remainingAhead,
          estimatedWaitSeconds: res.estimatedWaitSeconds,
          allowedAt: res.allowedAt,
          entryDeadline: res.entryDeadline,
        }),
      clearQueue: () =>
        set({
          queueToken: null,
          queueNumber: null,
          remainingAhead: null,
          estimatedWaitSeconds: null,
          allowedAt: null,
          entryDeadline: null,
        }),

      setReservedUntil: (until) => set({ reservedUntil: until }),
    }),
    {
      name: "booking-flow",
      storage: createJSONStorage(() => sessionStorage),
      // 새로고침 시 복구할 필드 (sectionId / selectedSeats만 휘발)
      partialize: (state) => ({
        open: state.open,
        step: state.step,
        eventId: state.eventId,
        matchId: state.matchId,
        queueToken: state.queueToken,
        queueNumber: state.queueNumber,
        remainingAhead: state.remainingAhead,
        estimatedWaitSeconds: state.estimatedWaitSeconds,
        allowedAt: state.allowedAt,
        entryDeadline: state.entryDeadline,
        reservedUntil: state.reservedUntil,
      }),
    },
  ),
);
