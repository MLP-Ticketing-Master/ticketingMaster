import { useMemo } from "react";
import { X, Timer } from "lucide-react";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { useBookingFlowStore } from "@/store";
import {
  useEventDetail,
  useMatches,
  useSeatGrades,
  useAdmissionTimer,
  formatCountdown,
} from "@/hooks";
import { formatShortDate, formatTime } from "@/lib/format";
import { SeatSidebar } from "./SeatSidebar";
import { ZoneStep } from "./steps/ZoneStep";
import { SeatStep } from "./steps/SeatStep";
import { QueueStep } from "./steps/QueueStep";
import { PaymentStep } from "./steps/PaymentStep";

export function BookingDialog() {
  const open = useBookingFlowStore((s) => s.open);
  const step = useBookingFlowStore((s) => s.step);
  const eventId = useBookingFlowStore((s) => s.eventId);
  const matchId = useBookingFlowStore((s) => s.matchId);
  const selectedSeats = useBookingFlowStore((s) => s.selectedSeats);
  const closeFlow = useBookingFlowStore((s) => s.closeFlow);
  const removeSeat = useBookingFlowStore((s) => s.removeSeat);
  const goToPayment = useBookingFlowStore((s) => s.goToPayment);

  const { data: event } = useEventDetail(eventId ?? 0);
  const { data: matches = [] } = useMatches(eventId ?? undefined);
  const { data: grades = [] } = useSeatGrades(eventId ?? 0);

  const match = matches.find((m) => m.id === matchId);

  const remaining = useAdmissionTimer();

  const total = useMemo(() => {
    if (selectedSeats.length === 0) return 0;
    const priceMap = new Map(grades.map((g) => [g.code, g.price]));
    return selectedSeats.reduce(
      (sum, s) => sum + (priceMap.get(s.gradeCode) ?? 0),
      0,
    );
  }, [selectedSeats, grades]);

  // 사이드바는 구역/좌석 선택 단계에서만 표시
  const showSidebar = step === "ZONE" || step === "SEAT";

  // 타이머는 대기열 이후 단계에서 표시
  const showTimer = remaining !== null && (step === "ZONE" || step === "SEAT");
  const isUrgent = remaining !== null && remaining <= 60;

  if (!event || !match) return null;

  return (
    <Dialog open={open} onOpenChange={(v) => !v && closeFlow()}>
      <DialogContent
        showCloseButton={false}
        className="!max-w-6xl gap-0 overflow-hidden p-0"
      >
        <header className="flex items-start justify-between bg-[#2D2F3E] px-8 py-6 text-white">
          <div>
            <h2 className="text-xl font-bold">{event.title}</h2>
            <p className="mt-1 text-sm text-gray-300">
              {formatShortDate(match.startAt)} {formatTime(match.startAt)}
            </p>
          </div>

          <div className="flex items-center gap-4">
            {/* 카운트다운 타이머 뱃지 */}
            {showTimer && (
              <div
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-bold tabular-nums transition-colors ${
                  isUrgent
                    ? "animate-pulse bg-red-500 text-white"
                    : "bg-white/10 text-white"
                }`}
              >
                <Timer className="h-4 w-4 shrink-0" />
                <span>{formatCountdown(remaining!)}</span>
              </div>
            )}

            <button
              type="button"
              onClick={closeFlow}
              className="text-gray-300 hover:text-white"
              aria-label="닫기"
            >
              <X className="h-6 w-6" />
            </button>
          </div>
        </header>

        <div className={showSidebar ? "grid grid-cols-[1fr_320px] bg-gray-50" : "bg-gray-50"}>
          <div className="max-h-[70vh] overflow-y-auto">
            {step === "QUEUE" && <QueueStep />}
            {step === "ZONE" && <ZoneStep />}
            {step === "SEAT" && <SeatStep />}
            {step === "PAYMENT" && (
              <PaymentStep
                selectedSeats={selectedSeats}
                grades={grades}
                total={total}
                matchId={matchId}
                onComplete={closeFlow}
              />
            )}
          </div>

          {showSidebar && (
            <SeatSidebar
              grades={grades}
              selected={selectedSeats}
              total={total}
              canSubmit={selectedSeats.length > 0}
              onRemove={removeSeat}
              onSubmit={goToPayment}
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
