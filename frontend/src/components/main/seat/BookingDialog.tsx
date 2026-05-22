import { useMemo } from "react";
import { AlertCircle, Timer, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { useBookingFlowStore } from "@/store";
import {
  useEventDetail,
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

  // matches는 event.matches 배열에서 직접 조회
  const match = event?.matches.find((m) => m.matchId === matchId);

  // 가격 정보는 event.seatGrades에서 가져옴
  // SeatSidebar / PaymentStep이 SeatGrade[] 타입을 기대하므로 어댑터 사용
  const grades = useMemo(() => {
    if (!event?.seatGrades) return [];
    return event.seatGrades.map((sg) => ({
      code: sg.gradeCode,
      name: `${sg.gradeCode}석`,
      price: sg.price,
      color: sg.colorHex,
      sortOrder: 0,
      remaining: 0,
    }));
  }, [event?.seatGrades]);

  const remaining = useAdmissionTimer();

  const total = useMemo(() => {
    if (selectedSeats.length === 0) return 0;
    const priceMap = new Map(grades.map((g) => [g.code, g.price]));
    return selectedSeats.reduce(
      (sum, s) => sum + (priceMap.get(s.gradeCode) ?? 0),
      0,
    );
  }, [selectedSeats, grades]);

  const isExpired = remaining === 0 && step !== "QUEUE";
  const showSidebar = !isExpired && (step === "ZONE" || step === "SEAT");
  const showTimer =
    !isExpired && remaining !== null && (step === "ZONE" || step === "SEAT");
  const isUrgent = remaining !== null && remaining <= 60;

  if (!event || !match) return null;

  return (
    <Dialog open={open} onOpenChange={(v) => !v && closeFlow()}>
      <DialogContent
        showCloseButton={false}
        className="!max-w-6xl gap-0 overflow-hidden p-0"
      >
        <header className="flex items-center justify-between border-b bg-white px-6 py-4">
          <div>
            <h2 className="text-base font-bold leading-tight">{event.title}</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {formatShortDate(match.startAt)} {formatTime(match.startAt)}
            </p>
          </div>

          <div className="flex items-center gap-3">
            {showTimer && (
              <div
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-bold tabular-nums transition-colors ${
                  isUrgent
                    ? "animate-pulse bg-red-500 text-white"
                    : "bg-violet-50 text-violet-600"
                }`}
              >
                <Timer className="h-4 w-4 shrink-0" />
                <span>{formatCountdown(remaining!)}</span>
              </div>
            )}

            <button
              type="button"
              onClick={closeFlow}
              className="text-muted-foreground hover:text-foreground"
              aria-label="닫기"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </header>

        <div className={showSidebar ? "grid grid-cols-[1fr_320px] items-stretch bg-gray-50" : "bg-gray-50"}>
          <div className="max-h-[85vh] min-h-[60vh] overflow-y-auto">
            {isExpired ? (
              <ExpiredView onClose={closeFlow} />
            ) : (
              <>
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
              </>
            )}
          </div>

          {showSidebar && (
            <div className="max-h-[85vh] min-h-[60vh] overflow-y-auto border-l">
              <SeatSidebar
                grades={grades}
                selected={selectedSeats}
                total={total}
                canSubmit={selectedSeats.length > 0}
                showGrades={step === "SEAT"}
                onRemove={removeSeat}
                onSubmit={goToPayment}
              />
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

function ExpiredView({ onClose }: { onClose: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center space-y-3 px-6">
      <div className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-red-100 text-red-600">
        <AlertCircle className="h-7 w-7" />
      </div>
      <h2 className="text-lg font-bold text-red-600">
        좌석 선택 시간이 만료되었습니다
      </h2>
      <p className="text-xs text-muted-foreground">
        다시 처음부터 진행해 주세요.
      </p>
      <Button onClick={onClose}>닫기</Button>
    </div>
  );
}
