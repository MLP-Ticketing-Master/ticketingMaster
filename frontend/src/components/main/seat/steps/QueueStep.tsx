import { useEffect, useRef } from "react";
import { CheckCircle, Clock, Megaphone, Users } from "lucide-react";
import { isAxiosError } from "axios";
import { toast } from "sonner";
import { Card } from "@/components/ui/card";
import { useBookingFlowStore } from "@/store";
import { useEnterQueueMutation, useQueueStatus } from "@/hooks";
import { ERROR_CODES, QUEUE_TOKEN_ERROR_CODES } from "@/lib/error";

export function QueueStep() {
  const matchId = useBookingFlowStore((s) => s.matchId);
  const queueToken = useBookingFlowStore((s) => s.queueToken);
  const queueNumber = useBookingFlowStore((s) => s.queueNumber);
  const remainingAhead = useBookingFlowStore((s) => s.remainingAhead);
  const estimatedWaitSeconds = useBookingFlowStore((s) => s.estimatedWaitSeconds);
  const goToZone = useBookingFlowStore((s) => s.goToZone);
  const closeFlow = useBookingFlowStore((s) => s.closeFlow);
  const clearQueue = useBookingFlowStore((s) => s.clearQueue);

  const enterMutation = useEnterQueueMutation();
  const enterCalledRef = useRef(false);

  // 진입 1회만 호출 (StrictMode 대응). store에 토큰이 있으면 enter 스킵
  useEffect(() => {
    if (!matchId || queueToken || enterCalledRef.current) return;
    enterCalledRef.current = true;
    enterMutation.mutate(matchId, {
      onSuccess: (data) => {
        // burst 게이트 통과 — 대기열 UI 거치지 않고 좌석 선택으로 직행
        if (data.status === "ALLOWED") {
          goToZone();
        }
      },
      onError: (error) => {
        const status = isAxiosError(error) ? error.response?.status : undefined;
        const code = isAxiosError(error) ? error.response?.data?.code : undefined;
        toast.error(getEnterErrorMessage(code, status));
        closeFlow();
      },
    });
  }, [matchId, queueToken, enterMutation, closeFlow, goToZone]);

  const statusQuery = useQueueStatus(matchId, queueToken);

  // ALLOWED 되면 좌석 선택으로 이동
  useEffect(() => {
    if (statusQuery.data?.status === "ALLOWED") {
      goToZone();
    }
  }, [statusQuery.data?.status, goToZone]);

  // 대기열 토큰 만료/누락/매치 불일치 → toast + 모달 닫기 (한 번만)
  const tokenErrorHandledRef = useRef(false);
  useEffect(() => {
    if (!statusQuery.isError || tokenErrorHandledRef.current) return;
    if (!isAxiosError(statusQuery.error)) return;
    const code = statusQuery.error.response?.data?.code;
    if (QUEUE_TOKEN_ERROR_CODES.includes(code)) {
      tokenErrorHandledRef.current = true;
      toast.error("대기열 정보가 만료되었습니다. 다시 시도해 주세요.");
      clearQueue();
      closeFlow();
    }
  }, [statusQuery.isError, statusQuery.error, clearQueue, closeFlow]);

  // 진입 중 / enter 에러 직후
  if (!queueToken || queueNumber === null) {
    return (
      <div className="py-16 text-center text-sm text-muted-foreground">
        대기열에 진입하는 중...
      </div>
    );
  }

  const estimatedMinutes = Math.max(
    1,
    Math.round((estimatedWaitSeconds ?? 0) / 60),
  );

  // ALLOWED 직전 안전망
  if (statusQuery.data?.status === "ALLOWED") {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center space-y-4">
        <div className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600 animate-bounce">
          <CheckCircle className="h-8 w-8" />
        </div>
        <h2 className="text-xl font-bold text-green-600">입장 가능</h2>
        <p className="text-sm text-muted-foreground">좌석 선택 페이지로 이동합니다.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6 px-6 py-10">
      <div className="text-center">
        <div className="mx-auto inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-violet-100 text-violet-600">
          <Clock className="h-6 w-6" />
        </div>
        <h2 className="mt-4 text-2xl font-bold">예매 대기 중입니다</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          현재 접속자가 많아 대기 중입니다.
          <br />
          잠시만 기다려주시면, 순서대로 자동으로 예매 페이지로 이동합니다.
        </p>
      </div>

      <Card className="p-8">
        <div className="text-center">
          <p className="text-sm font-medium">내 대기 순번</p>
          <p className="mt-3 text-5xl font-bold text-violet-600 tabular-nums leading-none">
            {queueNumber.toLocaleString()}
            <span className="ml-2 text-xl font-semibold">번째</span>
          </p>
        </div>

        <div className="my-6 h-px bg-border" />

        <div className="text-center">
          <p className="text-sm font-medium">예상 대기 시간</p>
          <p className="mt-3 text-3xl font-bold text-violet-600 tabular-nums leading-none">
            약 {estimatedMinutes}분
          </p>
        </div>
      </Card>

      <Card className="grid grid-cols-2 gap-4 p-5">
        <Stat
          icon={Users}
          label="내 앞 대기자"
          value={`${(remainingAhead ?? 0).toLocaleString()}명`}
        />
        <Stat
          icon={Clock}
          label="예상 대기 시간"
          value={`약 ${estimatedMinutes}분`}
        />
      </Card>

      <Card className="flex items-start gap-3 bg-gray-50 p-5">
        <Megaphone className="h-5 w-5 shrink-0 text-violet-600 mt-0.5" />
        <div>
          <p className="text-sm font-semibold">안내사항</p>
          <p className="mt-1 text-xs text-muted-foreground">
            입장 후 10분 안에 좌석 선택이 필요합니다.
          </p>
        </div>
      </Card>
    </div>
  );
}

function Stat({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Users;
  label: string;
  value: string;
}) {
  return (
    <div className="text-center">
      <Icon className="mx-auto h-5 w-5 text-violet-600" />
      <p className="mt-2 text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-base font-bold text-violet-600 tabular-nums">
        {value}
      </p>
    </div>
  );
}

function getEnterErrorMessage(
  code: string | undefined,
  status: number | undefined,
): string {
  if (status === 401) return "로그인이 필요합니다.";
  switch (code) {
    case ERROR_CODES.MATCH_NOT_FOUND:
      return "존재하지 않는 회차입니다.";
    case ERROR_CODES.BOOKING_NOT_OPEN:
      return "예매 가능 시간이 아닙니다.";
    case ERROR_CODES.USER_NOT_FOUND:
      return "사용자 정보를 찾을 수 없습니다.";
    default:
      return "대기열 진입에 실패했습니다.";
  }
}
