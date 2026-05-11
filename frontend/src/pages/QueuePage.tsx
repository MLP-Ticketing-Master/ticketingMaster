import { Activity, Clock, Megaphone, RefreshCw, Users } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";

const queueState = {
  myPosition: 1234,
  estimatedMinutes: 12,
  totalWaiting: 4567,
  throughputPerSec: 25,
  updatedAt: "10:30:15",
  progress: 35,
};

export default function QueuePage() {
  return (
    <div className="mx-auto max-w-3xl space-y-6 px-4 py-12">
      <div className="text-center">
        <div className="mx-auto inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600">
          <Clock className="h-7 w-7" />
        </div>
        <h1 className="mt-4 text-3xl font-bold">예매 대기 중입니다</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          현재 접속자가 많아 대기 중입니다.
          <br />
          잠시만 기다려주시면, 순서대로 자동으로 예매 페이지로 이동합니다.
        </p>
      </div>

      <Card className="space-y-6 p-8 text-center">
        <div>
          <p className="text-sm font-medium">내 대기 순번</p>
          <p className="mt-2 text-5xl font-bold text-indigo-600">
            {queueState.myPosition.toLocaleString()}
            <span className="ml-1 text-2xl">번째</span>
          </p>
        </div>
        <Separator />
        <div>
          <p className="text-sm font-medium">예상 대기 시간</p>
          <p className="mt-2 text-3xl font-bold text-indigo-600">
            약 {queueState.estimatedMinutes}분
          </p>
        </div>
      </Card>

      <Card className="grid grid-cols-2 gap-6 p-6 md:grid-cols-4">
        <Stat
          icon={Users}
          label="현재 대기 인원"
          value={`${queueState.totalWaiting.toLocaleString()}명`}
        />
        <Stat
          icon={Clock}
          label="예상 대기 시간"
          value={`약 ${queueState.estimatedMinutes}분`}
        />
        <Stat
          icon={Activity}
          label="현재 처리 속도"
          value={`1초당 ${queueState.throughputPerSec}명`}
        />
        <Stat icon={RefreshCw} label="최근 업데이트" value={queueState.updatedAt} />
      </Card>

      <Card className="flex gap-4 bg-indigo-50 p-6">
        <Megaphone className="h-6 w-6 shrink-0 text-indigo-600" />
        <div>
          <h3 className="font-semibold">안내사항</h3>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-muted-foreground">
            <li>새로고침을 하거나 창을 닫으면 대기 순번이 초기화됩니다.</li>
            <li>
              대기 중에도 페이지를 그대로 두시면 자동으로 예매 페이지로
              이동합니다.
            </li>
            <li>
              예매 페이지 진입 후 10분간 좌석을 선택하지 않으면 자동으로
              취소됩니다.
            </li>
          </ul>
        </div>
      </Card>

      <Card className="space-y-3 p-6">
        <p className="text-center text-sm text-muted-foreground">
          대기 순서가 되면 자동으로 다음 단계로 이동합니다.
        </p>
        <Progress value={queueState.progress} className="h-2 [&>div]:bg-indigo-600" />
        <p className="text-center text-sm font-semibold text-indigo-600">
          전체 진행률 {queueState.progress}%
        </p>
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
      <Icon className="mx-auto h-5 w-5 text-indigo-600" />
      <p className="mt-2 text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-base font-bold text-indigo-600">{value}</p>
    </div>
  );
}
