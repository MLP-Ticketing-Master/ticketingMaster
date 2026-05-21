import { useEffect, useState } from "react";
import { useBookingFlowStore } from "@/store";

/**
 * 좌석 선택 데드라인까지 남은 시간(초)을 반환
 * 백엔드 응답의 entryDeadline(ALLOWED 시점에 발급) 기준으로 카운트다운
 * entryDeadline이 없으면 null 반환
 */
export function useAdmissionTimer() {
  const entryDeadline = useBookingFlowStore((s) => s.entryDeadline);
  const [remaining, setRemaining] = useState<number | null>(null);

  useEffect(() => {
    if (!entryDeadline) {
      setRemaining(null);
      return;
    }

    const deadlineMs = new Date(entryDeadline).getTime();

    const tick = () => {
      const secondsLeft = Math.max(
        0,
        Math.floor((deadlineMs - Date.now()) / 1000),
      );
      setRemaining(secondsLeft);
    };

    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [entryDeadline]);

  return remaining;
}

/** mm:ss 형식으로 포맷 */
export function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}
