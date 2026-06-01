import { useMemo, useState } from "react";
import { AlertCircle, Timer, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
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
import { resolveErrorMessage } from "@/lib/error";
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
          toast.error(
            resolveErrorMessage(err, "좌석 점유에 실패했습니다. 다시 시도해주세요."),
          );
        },
      },
    );
  };

  // 결제 단계에서 점유 해제가 필요한지 — 이미 reserve 된 상태
  const shouldReleaseOnExit = step === "PAYMENT" && selectedSeats.length > 0;

  // 결제 단계에서 닫으려 하면 확인 다이얼로그 노출 — 그 외 단계는 바로 닫기
  const [confirmAbortOpen, setConfirmAbortOpen] = useState(false);
  const requestClose = () => {
    if (shouldReleaseOnExit) {
      setConfirmAbortOpen(true);
    } else {
      closeFlow();
    }
  };

  // 확인 후 실제 중단 — 점유 좌석 해제(백엔드가 PENDING 도 EXPIRED 처리) 후 종료
  const confirmAbort = () => {
    setConfirmAbortOpen(false);
    releaseSeats.mutate(
      selectedSeats.map((s) => s.seatId),
      {
        onSettled: () => {
          setReservedUntil(null);
          closeFlow();
        },
      },
    );
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
    <>
    <Dialog open={open} onOpenChange={(v) => !v && requestClose()}>
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
              onClick={requestClose}
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
              <ExpiredView onClose={closeFlow} />
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
                    eventTitle={event.title}
                    grades={grades}
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

    <AlertDialog open={confirmAbortOpen} onOpenChange={setConfirmAbortOpen}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>예매를 중단하시겠어요?</AlertDialogTitle>
          <AlertDialogDescription className="space-y-2">
            <span className="block">선택하신 좌석이 즉시 해제됩니다.</span>
            <span className="block">
              취소 후에는 대기열에 다시 입장해야 하며, 동일한 좌석을 다시
              선택하지 못할 수 있습니다.
            </span>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={releaseSeats.isPending}>
            계속 결제하기
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={confirmAbort}
            disabled={releaseSeats.isPending}
            className="bg-red-600 hover:bg-red-700"
          >
            예매 중단
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
    </>
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
