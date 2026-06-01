import { useCallback } from "react";
import { getMyPendingBooking } from "@/api";
import { useBookingFlowStore } from "@/store";

/**
 * 미완료 예매 이어하기 or 신규 시작 — 예매 진입점에서 공통 사용
 * - PENDING 있고 점유가 살아있으면 → 결제 단계로 복귀 (true 반환)
 * - 없거나 만료됐으면 → 기존 신규 흐름(대기열부터) (false 반환)
 */
export function useResumeOrStartBooking() {
  const restorePending = useBookingFlowStore((s) => s.restorePending);
  const openFlow = useBookingFlowStore((s) => s.openFlow);

  return useCallback(
    async (eventId: number, matchId: number): Promise<boolean> => {
      let pending: Awaited<ReturnType<typeof getMyPendingBooking>> = null;
      try {
        pending = await getMyPendingBooking(matchId);
      } catch {
        // 조회 실패 시 신규 흐름으로 폴백 — 사용자 진입 자체를 막지 않기 위함
        pending = null;
      }

      // reservedUntil 이 미래일 때만 결제 복귀 — null/과거면 좌석이 이미 풀렸을 수 있어
      // 신규 흐름으로 보냄 (만료 좌석으로 결제 시도하는 상황 차단)
      const reservedUntil = pending?.reservedUntil ?? null;
      const stillReserved =
        reservedUntil != null && new Date(reservedUntil).getTime() > Date.now();

      if (pending && stillReserved) {
        restorePending({
          eventId,
          matchId,
          bookingId: pending.bookingId,
          bookingNumber: pending.bookingNumber,
          reservedUntil,
          // 복귀 경로엔 좌석 그리드 정보 불필요 — 결제 화면 표시용 최소 필드만 채움
          // colorHex 는 PaymentStep 에서 gradeCode 로 보완
          selectedSeats: pending.seats.map((s) => ({
            seatId: s.seatId,
            seatCode: s.seatCode,
            gradeCode: s.gradeCode,
            price: s.seatPrice,
            rowLabel: "",
            seatNo: 0,
            colorHex: "",
            status: "RESERVED",
          })),
        });
        return true;
      }

      openFlow({ eventId, matchId });
      return false;
    },
    [restorePending, openFlow],
  );
}
