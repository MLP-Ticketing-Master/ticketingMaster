import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Loader2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useBookingFlowStore } from "@/store";
import { getMyPendingBooking, releaseSeats } from "@/api";

export default function PaymentFailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  // reset 하지 않음 — store 의 eventId/matchId 로 결제 단계 복귀를 지원하기 위함
  const eventId = useBookingFlowStore((s) => s.eventId);
  const matchId = useBookingFlowStore((s) => s.matchId);
  const reset = useBookingFlowStore((s) => s.reset);
  const [canceling, setCanceling] = useState(false);

  // 토스 fail redirect 시 message 쿼리로 실제 실패 사유가 들어옴 (없으면 기본 문구)
  const message = params.get("message") ?? "결제가 취소되었거나 실패했습니다.";

  // [다시 시도] — 매치 페이지로 보내 미완료 예매를 결제 단계로 복귀시킴
  const handleRetry = () => {
    if (eventId && matchId) {
      navigate(`/events/${eventId}?resumeBooking=1&matchId=${matchId}`);
    } else {
      navigate("/");
    }
  };

  // [취소] — 점유 좌석 해제(+백엔드가 PENDING booking EXPIRED 처리) 후 평소 흐름으로
  const handleCancel = async () => {
    if (!matchId) {
      reset();
      navigate(eventId ? `/events/${eventId}` : "/");
      return;
    }
    setCanceling(true);
    try {
      const pending = await getMyPendingBooking(matchId);
      if (pending) {
        await releaseSeats(
          matchId,
          pending.seats.map((s) => s.seatId),
        );
      }
    } catch {
      // 해제 실패해도 점유는 만료 시 스케줄러가 정리 — 사용자 흐름은 계속 진행
    } finally {
      reset();
      navigate(eventId ? `/events/${eventId}` : "/");
    }
  };

  return (
    <Card className="mx-auto max-w-md mt-20 p-10 text-center space-y-4">
      <div className="mx-auto inline-flex h-20 w-20 items-center justify-center rounded-full bg-red-100 text-red-600">
        <XCircle className="h-10 w-10" />
      </div>
      <h2 className="text-2xl font-bold text-red-600">결제 실패</h2>
      <p className="text-sm text-muted-foreground">{message}</p>
      <div className="flex justify-center gap-2 pt-2">
        <Button variant="outline" onClick={handleCancel} disabled={canceling}>
          {canceling ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              취소 중...
            </>
          ) : (
            "예매 취소"
          )}
        </Button>
        <Button onClick={handleRetry} disabled={canceling}>
          다시 시도
        </Button>
      </div>
    </Card>
  );
}
