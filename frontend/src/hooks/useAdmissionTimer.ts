import { useEffect, useState } from "react";
import { useBookingFlowStore } from "@/store";

const computeRemaining = (deadline: string | null): number | null => {
  if (!deadline) return null;
  return Math.max(
    0,
    Math.floor((new Date(deadline).getTime() - Date.now()) / 1000),
  );
};

/**
 * ISO 시각 기준 카운트다운 — 남은 초 반환 (null 이면 비활성)
 */
export function useDeadlineTimer(deadline: string | null) {
  const [remaining, setRemaining] = useState<number | null>(() =>
    computeRemaining(deadline),
  );

  useEffect(() => {
    // deadline 자체가 변경된 직후엔 즉시 갱신 (1초 간격을 기다리지 않도록)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setRemaining(computeRemaining(deadline));

    if (!deadline) return;
    const id = setInterval(() => {
      setRemaining(computeRemaining(deadline));
    }, 1000);
    return () => clearInterval(id);
  }, [deadline]);

  return remaining;
}

/**
 * 큐 ALLOWED 시점부터 좌석 선택 데드라인까지 카운트다운 (10분)
 */
export function useAdmissionTimer() {
  const entryDeadline = useBookingFlowStore((s) => s.entryDeadline);
  return useDeadlineTimer(entryDeadline);
}

/**
 * 좌석 점유 시점부터 결제 데드라인까지 카운트다운 (7분)
 */
export function usePaymentTimer() {
  const reservedUntil = useBookingFlowStore((s) => s.reservedUntil);
  return useDeadlineTimer(reservedUntil);
}

/** mm:ss 형식으로 포맷 */
export function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}
