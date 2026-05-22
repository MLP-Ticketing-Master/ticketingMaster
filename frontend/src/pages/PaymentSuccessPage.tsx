import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { AxiosError } from "axios";
import { CheckCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useConfirmPaymentMutation } from "@/hooks";
import { useBookingFlowStore } from "@/store";

export default function PaymentSuccessPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const confirm = useConfirmPaymentMutation();

  // 결제 흐름 종료 — 다이얼로그 자동 복원 방지
  useEffect(() => {
    useBookingFlowStore.getState().reset();
  }, []);

  // 마운트 시점의 URL/localStorage 값을 한 번만 캡처
  // (onSuccess 에서 lastBookingId 를 제거하면 재렌더 시 bookingId 가 0 으로 바뀌어
  //  invalidParams=true 로 흘러가 성공 UI 가 사라지는 버그 방지)
  const [initial] = useState(() => {
    const paymentKey = params.get("paymentKey");
    const orderId = params.get("orderId");
    const amount = Number(params.get("amount") ?? 0);
    const bookingIdParam = params.get("bookingId");
    const bookingId = bookingIdParam
      ? Number(bookingIdParam)
      : Number(localStorage.getItem("lastBookingId") ?? 0);
    return { paymentKey, orderId, amount, bookingId };
  });
  const { paymentKey, orderId, amount, bookingId } = initial;

  const invalidParams = !paymentKey || !orderId || !amount || !bookingId;
  const calledRef = useRef(false);

  useEffect(() => {
    // 멱등성 — StrictMode/리렌더 대비 한 번만 호출
    if (calledRef.current || invalidParams) return;
    calledRef.current = true;
    confirm.mutate(
      { bookingId, paymentKey: paymentKey!, orderId: orderId!, amount },
      {
        onSuccess: () => {
          localStorage.removeItem("lastBookingId");
        },
      },
    );
  }, [invalidParams, bookingId, paymentKey, orderId, amount, confirm]);

  // URL 파라미터 자체가 잘못된 경우
  if (invalidParams) {
    return (
      <Card className="mx-auto max-w-md mt-20 p-10 text-center space-y-4">
        <p className="text-lg font-bold text-red-600">결제 정보 오류</p>
        <p className="text-sm text-muted-foreground">
          결제 정보가 올바르지 않습니다.
        </p>
        <div className="flex justify-center gap-2 pt-2">
          <Button variant="outline" onClick={() => navigate("/")}>
            홈으로
          </Button>
        </div>
      </Card>
    );
  }

  // 백엔드 confirm 호출 실패
  if (confirm.isError) {
    const axiosErr = confirm.error as AxiosError<{ message?: string }>;
    const errMsg =
      axiosErr.response?.data?.message ?? "결제 승인에 실패했습니다.";
    return (
      <Card className="mx-auto max-w-md mt-20 p-10 text-center space-y-4">
        <p className="text-lg font-bold text-red-600">결제 승인 실패</p>
        <p className="text-sm text-muted-foreground">{errMsg}</p>
        <div className="flex justify-center gap-2 pt-2">
          <Button variant="outline" onClick={() => navigate("/")}>
            홈으로
          </Button>
          <Button onClick={() => navigate("/my/bookings")}>예매 내역</Button>
        </div>
      </Card>
    );
  }

  // 승인 진행 중 (또는 호출 직전)
  if (!confirm.isSuccess) {
    return (
      <div className="mx-auto max-w-md px-6 py-20 text-center text-muted-foreground">
        결제 승인 중...
      </div>
    );
  }

  // 승인 성공
  return (
    <Card className="mx-auto max-w-md mt-20 p-10 text-center space-y-4">
      <div className="mx-auto inline-flex h-20 w-20 items-center justify-center rounded-full bg-green-100 text-green-600">
        <CheckCircle className="h-10 w-10" />
      </div>
      <h2 className="text-2xl font-bold text-green-600">예매 완료!</h2>
      <p className="text-sm text-muted-foreground">
        결제가 정상적으로 처리되었습니다.
      </p>
      <div className="flex justify-center gap-2 pt-2">
        <Button variant="outline" onClick={() => navigate("/")}>
          홈으로
        </Button>
        <Button onClick={() => navigate("/my/bookings")}>예매 내역</Button>
      </div>
    </Card>
  );
}
