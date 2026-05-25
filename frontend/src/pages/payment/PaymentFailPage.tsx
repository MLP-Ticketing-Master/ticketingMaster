import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useBookingFlowStore } from "@/store";

export default function PaymentFailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  // 결제 흐름 종료 — 다이얼로그 자동 복원 방지
  useEffect(() => {
    useBookingFlowStore.getState().reset();
  }, []);

  // 토스 fail redirect 시 code, message 쿼리로 들어옴
  const code = params.get("code");
  const message = params.get("message") ?? "결제가 취소되었거나 실패했습니다.";

  return (
    <Card className="mx-auto max-w-md mt-20 p-10 text-center space-y-4">
      <div className="mx-auto inline-flex h-20 w-20 items-center justify-center rounded-full bg-red-100 text-red-600">
        <XCircle className="h-10 w-10" />
      </div>
      <h2 className="text-2xl font-bold text-red-600">결제 실패</h2>
      <p className="text-sm text-muted-foreground">{message}</p>
      {code && (
        <p className="text-xs text-muted-foreground">에러 코드: {code}</p>
      )}
      <div className="flex justify-center gap-2 pt-2">
        <Button variant="outline" onClick={() => navigate("/")}>
          홈으로
        </Button>
        <Button onClick={() => navigate(-1)}>다시 시도</Button>
      </div>
    </Card>
  );
}
