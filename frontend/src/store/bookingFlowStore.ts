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

  // 생성된 PENDING booking — 결제 단계에서 createBooking 재호출 방지 + 이어서 결제용
  bookingId: number | null;
  bookingNumber: string | null;

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

  // 생성된 booking 식별자 저장 (createBooking 응답)
  setBooking: (bookingId: number, bookingNumber: string) => void;

  // 미완료 예매 복원 — 곧장 결제 단계로 진입시키기 위함
  restorePending: (params: {
    eventId: number;
    matchId: number;
    bookingId: number;
    bookingNumber: string;
    reservedUntil: string | null;
    selectedSeats: Seat[];
  }) => void;
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
  bookingId: null,
  bookingNumber: null,
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
                allowedAt: prev.allowedAt,
                entryDeadline: prev.entryDeadline,
              }
            : {}),
        });
      },
      closeFlow: () =>
        set({
          // 큐 세션 상태(queueToken / queueNumber / remainingAhead /
          // estimatedWaitSeconds / allowedAt / entryDeadline)는 보존 —
          // 백엔드 토큰 TTL 이 살아있는 동안 재진입 시 그대로 재개. 토큰이
          // 서버상 만료된 경우엔 다음 statusQuery 에서 token error 가 떨어지고
          // QueueStep 이 clearQueue() 로 정리함
          open: false,
          step: "ZONE",
          sectionId: null,
          selectedSeats: [],
          reservedUntil: null,
          bookingId: null,
          bookingNumber: null,
        }),
      goToSeat: (sectionId) => set({ step: "SEAT", sectionId }),
      goBackToZone: () => set({ step: "ZONE", sectionId: null }),
      // 결제 단계에서 좌석 다시 선택 — 선택 좌석 비우고 SEAT 단계로 복귀
      // 기존 PENDING booking 은 해제(release)로 EXPIRED 처리되므로 식별자도 비움
      goBackToSeatStep: () =>
        set({
          step: "SEAT",
          selectedSeats: [],
          bookingId: null,
          bookingNumber: null,
        }),
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

      setBooking: (bookingId, bookingNumber) =>
        set({ bookingId, bookingNumber }),

      // 미완료 예매 복원 — 큐 상태는 비우고 곧장 PAYMENT 단계로 진입
      restorePending: ({
        eventId,
        matchId,
        bookingId,
        bookingNumber,
        reservedUntil,
        selectedSeats,
      }) =>
        set({
          ...initial,
          open: true,
          step: "PAYMENT",
          eventId,
          matchId,
          bookingId,
          bookingNumber,
          reservedUntil,
          selectedSeats,
        }),
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
        bookingId: state.bookingId,
        bookingNumber: state.bookingNumber,
      }),
    },
  ),
);
