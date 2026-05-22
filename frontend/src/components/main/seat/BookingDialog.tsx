import { useMemo } from "react";
import { AxiosError } from "axios";
import { AlertCircle, Timer, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { useBookingFlowStore } from "@/store";
import {
  useEventDetail,
  useSeatSections,
  useAdmissionTimer,
  usePaymentTimer,
  formatCountdown,
  useReserveSeatsMutation,
  useReleaseSeatsMutation,
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
  const goBackToSeatStep = useBookingFlowStore((s) => s.goBackToSeatStep);
  const setReservedUntil = useBookingFlowStore((s) => s.setReservedUntil);

  const { data: event } = useEventDetail(eventId ?? 0);
  const { data: sectionList } = useSeatSections(matchId);
  const grades = sectionList?.gradeAvailability ?? [];

  // match는 event.matches 배열에서 직접 조회
  const match = event?.matches.find((m) => m.matchId === matchId);

  // 단계별 타이머: ZONE/SEAT 는 entryDeadline(10분), PAYMENT 는 reservedUntil(7분)
  const admissionRemaining = useAdmissionTimer();
  const paymentRemaining = usePaymentTimer();
  const remaining =
    step === "PAYMENT" ? paymentRemaining : admissionRemaining;

  const reserveSeats = useReserveSeatsMutation(matchId ?? 0);
  const releaseSeats = useReleaseSeatsMutation(matchId ?? 0);

  // 좌석 단가 합산 (백엔드 SeatItem 에 price 포함)
  const total = useMemo(
    () => selectedSeats.reduce((sum, s) => sum + s.price, 0),
    [selectedSeats],
  );

  // 만료: ZONE/SEAT 는 entryDeadline 만료, PAYMENT 는 reservedUntil 만료
  const isExpired = remaining === 0 && step !== "QUEUE";
  const showSidebar = !isExpired && (step === "ZONE" || step === "SEAT");
  // PAYMENT 단계에서도 타이머 표시 — reservedUntil 기준 7분 카운트
  const showTimer = !isExpired && remaining !== null && step !== "QUEUE";
  const isUrgent = remaining !== null && remaining <= 60;

  // 좌석 선택 완료 → 백엔드 점유 요청 → 결제 단계 진입
  const handleSelectComplete = () => {
    if (!matchId || selectedSeats.length === 0) return;
    reserveSeats.mutate(
      selectedSeats.map((s) => s.seatId),
      {
        onSuccess: (res) => {
          setReservedUntil(res.reservedUntil);
          goToPayment();
        },
        onError: (err) => {
          const axiosErr = err as AxiosError<{ message?: string }>;
          const msg =
            axiosErr.response?.data?.message ??
            "좌석 점유에 실패했습니다. 다시 시도해주세요.";
          toast.error(msg);
        },
      },
    );
  };

  // 결제 단계에서 점유 해제가 필요한지 — 이미 reserve 된 상태
  const shouldReleaseOnExit = step === "PAYMENT" && selectedSeats.length > 0;

  // 결제 단계에서 닫기 — 점유 좌석을 해제하고 종료
  const handleClose = () => {
    if (shouldReleaseOnExit) {
      releaseSeats.mutate(
        selectedSeats.map((s) => s.seatId),
        {
          onSettled: () => {
            setReservedUntil(null);
            closeFlow();
          },
        },
      );
    } else {
      closeFlow();
    }
  };

  // 결제 단계에서 좌석 다시 선택 — 점유 해제 후 SEAT 단계로 복귀
  const handleBackToSeat = () => {
    if (shouldReleaseOnExit) {
      releaseSeats.mutate(
        selectedSeats.map((s) => s.seatId),
        {
          onSettled: () => {
            setReservedUntil(null);
            goBackToSeatStep();
          },
        },
      );
    } else {
      goBackToSeatStep();
    }
  };

  if (!event || !match) return null;

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent
        showCloseButton={false}
        className="!max-w-6xl gap-0 overflow-hidden p-0"
        // 토스 위젯이 body 포털로 떠도 다이얼로그가 자동 닫히지 않도록 차단
        // 닫기는 명시적 X 버튼 / "좌석 다시 선택" 으로만
        onPointerDownOutside={(e) => e.preventDefault()}
        onInteractOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
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
              onClick={handleClose}
              className="text-muted-foreground hover:text-foreground"
              aria-label="닫기"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </header>

        <div
          className={
            showSidebar
              ? "grid grid-cols-[1fr_320px] items-stretch bg-gray-50"
              : "bg-gray-50"
          }
        >
          <div className="max-h-[85vh] min-h-[60vh] overflow-y-auto">
            {isExpired ? (
              <ExpiredView onClose={handleClose} />
            ) : (
              <>
                {step === "QUEUE" && <QueueStep />}
                {step === "ZONE" && <ZoneStep />}
                {step === "SEAT" && <SeatStep />}
                {step === "PAYMENT" && (
                  <PaymentStep
                    selectedSeats={selectedSeats}
                    total={total}
                    matchId={matchId}
                    onBack={handleBackToSeat}
                    isReleasing={releaseSeats.isPending}
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
                canSubmit={selectedSeats.length > 0 && !reserveSeats.isPending}
                showGrades={step === "SEAT"}
                onRemove={removeSeat}
                onSubmit={handleSelectComplete}
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
