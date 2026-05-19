import { useEffect, useState } from "react";
import { useBookingFlowStore } from "@/store";

const SEAT_LIMIT_SEC = 10 * 60; // 좌석 선택 제한: 10분

/**
 * 대기열 입장 후 남은 시간(초)을 반환합니다.
 * admittedAt이 없으면 null 반환.
 */
export function useAdmissionTimer() {
  const admittedAt = useBookingFlowStore((s) => s.admittedAt);
  const [remaining, setRemaining] = useState<number | null>(null);

  useEffect(() => {
    if (!admittedAt) {
      setRemaining(null);
      return;
    }

    const tick = () => {
      const elapsed = Math.floor((Date.now() - admittedAt) / 1000);
      setRemaining(Math.max(0, SEAT_LIMIT_SEC - elapsed));
    };

    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [admittedAt]);

  return remaining;
}

/** mm:ss 형식으로 포맷 */
export function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}
