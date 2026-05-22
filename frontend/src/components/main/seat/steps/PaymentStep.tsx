import { useState } from "react";
import {
  ArrowLeft,
  CreditCard,
  CheckCircle,
  Loader2,
  MapPin,
  Tag,
  AlertCircle,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { useCreateBookingMutation } from "@/hooks";
import { formatPrice } from "@/lib/format";
import type { Seat } from "@/types";

interface Props {
  selectedSeats: Seat[];
  total: number;
  matchId: number | null;
  onComplete: () => void;
  onBack: () => void;
  isReleasing: boolean;
}

export function PaymentStep({
  selectedSeats,
  total,
  matchId,
  onComplete,
  onBack,
  isReleasing,
}: Props) {
  const createBooking = useCreateBookingMutation();
  const [done, setDone] = useState(false);

  const handlePayment = () => {
    if (!matchId) return;
    createBooking.mutate(
      { matchId, seatIds: selectedSeats.map((s) => s.seatId) },
      {
        onSuccess: () => {
          setDone(true);
          toast.success("예매가 완료되었습니다!");
          setTimeout(() => {
            onComplete();
          }, 1800);
        },
        onError: () => {
          toast.error("결제에 실패했습니다. 다시 시도해주세요.");
        },
      },
    );
  };

  if (done) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center space-y-4">
        <div className="mx-auto inline-flex h-20 w-20 items-center justify-center rounded-full bg-green-100 text-green-600">
          <CheckCircle className="h-10 w-10" />
        </div>
        <h2 className="text-2xl font-bold text-green-600">예매 완료!</h2>
        <p className="text-muted-foreground text-sm">잠시 후 창이 닫힙니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-5 px-6 py-8 max-w-lg mx-auto">
      {/* 뒤로 가기 */}
      <Button
        variant="outline"
        size="sm"
        onClick={onBack}
        disabled={isReleasing || createBooking.isPending}
        className="gap-2"
      >
        <ArrowLeft className="h-4 w-4" />
        {isReleasing ? "좌석 해제 중..." : "좌석 다시 선택"}
      </Button>

      {/* 헤더 */}
      <div className="flex items-center gap-3">
        <div className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
          <CreditCard className="h-5 w-5" />
        </div>
        <div>
          <h2 className="text-lg font-bold">결제 확인</h2>
          <p className="text-sm text-muted-foreground">
            선택하신 좌석 정보를 확인해주세요.
          </p>
        </div>
      </div>

      {/* 결제 시간 경고 */}
      <div className="flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-2.5">
        <AlertCircle className="h-4 w-4 shrink-0 text-amber-500" />
        <p className="text-xs text-amber-700">
          <span className="font-bold">7분 이내</span>에 결제를 완료해 주세요.
          시간이 초과되면 선택한 좌석이 자동으로 해제됩니다.
        </p>
      </div>

      {/* 좌석 정보 */}
      <Card className="p-5 space-y-3">
        <h3 className="text-sm font-semibold flex items-center gap-2">
          <MapPin className="h-4 w-4 text-indigo-500" />
          선택 좌석 ({selectedSeats.length}석)
        </h3>
        <ul className="space-y-2">
          {selectedSeats.map((seat) => (
            <li
              key={seat.seatId}
              className="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-2.5 text-sm"
            >
              <div className="flex items-center gap-2">
                <span
                  className="h-2.5 w-2.5 rounded-sm"
                  style={{ backgroundColor: `#${seat.colorHex}` }}
                />
                <span className="font-medium">
                  {seat.gradeCode}석 {seat.rowLabel}
                  {seat.seatNo}
                </span>
              </div>
              <span className="text-muted-foreground">
                {formatPrice(seat.price)}
              </span>
            </li>
          ))}
        </ul>
      </Card>

      {/* 결제 금액 */}
      <Card className="p-5 space-y-3">
        <h3 className="text-sm font-semibold flex items-center gap-2">
          <Tag className="h-4 w-4 text-indigo-500" />
          결제 금액
        </h3>
        <div className="space-y-2 text-sm">
          {selectedSeats.map((seat) => (
            <div
              key={seat.seatId}
              className="flex justify-between text-muted-foreground"
            >
              <span>
                {seat.rowLabel}
                {seat.seatNo}번 좌석
              </span>
              <span>{formatPrice(seat.price)}</span>
            </div>
          ))}
        </div>
        <Separator />
        <div className="flex items-center justify-between font-bold">
          <span>총 결제금액</span>
          <span className="text-xl text-[#054EFD]">{formatPrice(total)}</span>
        </div>
      </Card>

      {/* 결제 버튼 */}
      <Button
        size="lg"
        onClick={handlePayment}
        disabled={createBooking.isPending || selectedSeats.length === 0}
        className="w-full bg-[#054EFD] hover:bg-[#3C76FE] text-white text-base font-bold disabled:bg-gray-200 disabled:text-gray-400 h-14"
      >
        {createBooking.isPending ? (
          <>
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            결제 처리 중...
          </>
        ) : (
          <>
            <CreditCard className="mr-2 h-5 w-5" />
            {formatPrice(total)} 결제하기
          </>
        )}
      </Button>
    </div>
  );
}
