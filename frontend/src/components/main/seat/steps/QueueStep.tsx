import { useEffect, useState } from "react";
import { CheckCircle, Clock, Megaphone, Users } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { useBookingFlowStore } from "@/store";

// TODO: 실제 대기열 API로 교체 필요
const MOCK_TOTAL_WAITING = 1249;
const MOCK_INITIAL_POSITION = 1250;
const MOCK_ESTIMATED_MINUTES = 12;
const SIMULATE_QUEUE_MS = 8000; // 데모용: 8초 후 입장 허용

export function QueueStep() {
  const goToZone = useBookingFlowStore((s) => s.goToZone ?? s.goBackToZone ?? (() => s.goToSeat(0)));
  const goActuallyToZone = useBookingFlowStore((s) => s.goToZone);

  const [position, setPosition] = useState(MOCK_INITIAL_POSITION);
  const [admitted, setAdmitted] = useState(false);
  const [countdown, setCountdown] = useState(3);

  // 대기순번 카운트다운 시뮬레이션
  useEffect(() => {
    const interval = setInterval(() => {
      setPosition((prev) => Math.max(0, prev - Math.floor(Math.random() * 15 + 5)));
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  // 일정 시간 후 입장 허용 (데모)
  useEffect(() => {
    const timer = setTimeout(() => {
      setAdmitted(true);
    }, SIMULATE_QUEUE_MS);
    return () => clearTimeout(timer);
  }, []);

  // 입장 허용 후 카운트다운 → 자동 이동
  useEffect(() => {
    if (!admitted) return;
    if (countdown <= 0) {
      // goToZone action
      if (goActuallyToZone) goActuallyToZone();
      return;
    }
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [admitted, countdown, goActuallyToZone]);

  const progress = Math.round(
    ((MOCK_INITIAL_POSITION - position) / MOCK_INITIAL_POSITION) * 100,
  );
  const estimatedMinutes = Math.max(
    1,
    Math.round((position / MOCK_INITIAL_POSITION) * MOCK_ESTIMATED_MINUTES),
  );

  if (admitted) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center space-y-6">
        <div className="mx-auto inline-flex h-20 w-20 items-center justify-center rounded-full bg-green-100 text-green-600 animate-bounce">
          <CheckCircle className="h-10 w-10" />
        </div>
        <div>
          <h2 className="text-2xl font-bold text-green-600">입장 가능</h2>
          <p className="mt-2 text-muted-foreground">
            예매 페이지로 이동합니다.
          </p>
        </div>
        <Card className="p-6 w-64 text-center space-y-2">
          <p className="text-sm text-muted-foreground">제한 시간</p>
          <p className="text-4xl font-bold text-green-600">10:00</p>
          <p className="text-xs text-muted-foreground">
            {countdown}초 후 자동으로 이동합니다
          </p>
        </Card>
        {goActuallyToZone && (
          <button
            type="button"
            onClick={goActuallyToZone}
            className="mt-2 text-sm font-semibold text-indigo-600 underline underline-offset-2 hover:text-indigo-800"
          >
            지금 바로 이동하기
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-5 px-6 py-8">
      {/* 헤더 */}
      <div className="text-center">
        <div className="mx-auto inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600">
          <Clock className="h-7 w-7" />
        </div>
        <h2 className="mt-4 text-2xl font-bold">대기열 접속 중</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          현재 접속자가 많아 대기 중입니다.
          <br />
          잠시만 기다려주시면 순서대로 예매 페이지로 이동합니다.
        </p>
      </div>

      {/* 메인 카드 - 이미지 디자인 참고 */}
      <Card className="mx-auto max-w-sm p-8 text-center space-y-5 border-2 border-indigo-100">
        <div>
          <p className="text-sm font-medium text-muted-foreground">현재 대기 순번</p>
          <p className="mt-1 text-6xl font-extrabold text-indigo-600">
            {position.toLocaleString()}
          </p>
          <p className="text-lg font-semibold text-indigo-500">번</p>
        </div>

        <div className="h-px bg-gray-100" />

        <div className="grid grid-cols-2 gap-4 text-center">
          <div>
            <Users className="mx-auto h-4 w-4 text-muted-foreground mb-1" />
            <p className="text-xs text-muted-foreground">내 앞 대기자</p>
            <p className="text-base font-bold">{(position - 1).toLocaleString()}명</p>
          </div>
          <div>
            <Clock className="mx-auto h-4 w-4 text-muted-foreground mb-1" />
            <p className="text-xs text-muted-foreground">예상 대기시간</p>
            <p className="text-base font-bold">{estimatedMinutes}분</p>
          </div>
        </div>

        <div className="space-y-2">
          <Progress value={progress} className="h-2.5 [&>div]:bg-indigo-500" />
          <p className="text-xs text-muted-foreground">
            {progress}% 진행됨
          </p>
        </div>
      </Card>

      {/* 안내 카드 */}
      <Card className="flex gap-4 bg-amber-50 border-amber-200 p-5">
        <Megaphone className="h-5 w-5 shrink-0 text-amber-600 mt-0.5" />
        <div className="text-sm">
          <h3 className="font-semibold text-amber-800">안내사항</h3>
          <ul className="mt-2 list-disc space-y-1 pl-4 text-amber-700">
            <li>새로고침하거나 창을 닫으면 대기 순번이 초기화됩니다.</li>
            <li>대기 중에는 페이지를 그대로 유지해 주세요.</li>
            <li>입장 허용 후 10분 안에 좌석을 선택하지 않으면 자동 취소됩니다.</li>
          </ul>
        </div>
      </Card>
    </div>
  );
}
