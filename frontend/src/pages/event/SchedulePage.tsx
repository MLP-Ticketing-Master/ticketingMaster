import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { formatTime } from "@/lib/format";
import { MOCK_SCHEDULE } from "@/lib/mock";
import { cn } from "@/lib/utils";
import type {
  LeagueCode,
  MatchStatus,
  ScheduleTeam,
  ScheduledMatch,
} from "@/types";

// 상단 상태 탭
const STATUS_TABS: { key: "ALL" | MatchStatus; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "LIVE", label: "진행 중" },
  { key: "SCHEDULED", label: "예정" },
  { key: "FINISHED", label: "종료" },
];

// 리그 필터 — mock 데이터에 등장하는 리그만 우선 노출
const LEAGUE_FILTERS: { key: "ALL" | LeagueCode; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "LCK", label: "LCK" },
  { key: "LPL", label: "LPL" },
  { key: "MSI", label: "MSI" },
  { key: "VCT", label: "VCT" },
  { key: "OWCS", label: "OWCS" },
];

const STATUS_BADGE: Record<MatchStatus, { label: string; className: string }> =
  {
    LIVE: { label: "LIVE", className: "bg-red-500 text-white" },
    SCHEDULED: { label: "예정", className: "bg-blue-100 text-blue-700" },
    FINISHED: { label: "종료", className: "bg-gray-200 text-gray-600" },
  };

export default function SchedulePage() {
  const [statusTab, setStatusTab] = useState<"ALL" | MatchStatus>("ALL");
  const [leagueTab, setLeagueTab] = useState<"ALL" | LeagueCode>("ALL");

  // 필터링 + 시간순 정렬 + 날짜별 그룹핑
  const grouped = useMemo(() => {
    const filtered = MOCK_SCHEDULE.filter((m) => {
      if (statusTab !== "ALL" && m.status !== statusTab) return false;
      if (leagueTab !== "ALL" && m.leagueCode !== leagueTab) return false;
      return true;
    }).sort(
      (a, b) =>
        new Date(a.startAt).getTime() - new Date(b.startAt).getTime(),
    );

    const map = new Map<string, ScheduledMatch[]>();
    for (const m of filtered) {
      const key = m.startAt.slice(0, 10);
      const list = map.get(key) ?? [];
      list.push(m);
      map.set(key, list);
    }
    return Array.from(map.entries());
  }, [statusTab, leagueTab]);

  return (
    <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 sm:py-10">
      <header className="mb-6 sm:mb-8">
        <h1 className="text-2xl font-bold sm:text-3xl">대회 일정</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          E스포츠 경기 일정과 결과를 확인하세요
        </p>
      </header>

      {/* 상태 탭 */}
      <div className="mb-3 flex gap-1 overflow-x-auto border-b">
        {STATUS_TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setStatusTab(t.key)}
            className={cn(
              "shrink-0 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors",
              statusTab === t.key
                ? "border-[#054EFD] text-[#054EFD]"
                : "border-transparent text-muted-foreground hover:text-foreground",
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* 리그 필터 */}
      <div className="mb-6 flex gap-2 overflow-x-auto pb-1">
        {LEAGUE_FILTERS.map((l) => (
          <Button
            key={l.key}
            size="sm"
            variant={leagueTab === l.key ? "default" : "outline"}
            onClick={() => setLeagueTab(l.key)}
            className={cn(
              "shrink-0 rounded-full",
              leagueTab === l.key && "bg-[#316DFD] hover:bg-[#1C5EFD]",
            )}
          >
            {l.label}
          </Button>
        ))}
      </div>

      {/* 매치 리스트 */}
      {grouped.length === 0 ? (
        <div className="py-20 text-center text-sm text-muted-foreground">
          해당 조건의 경기가 없습니다.
        </div>
      ) : (
        <div className="space-y-8">
          {grouped.map(([date, matches]) => (
            <section key={date}>
              <DateHeader date={date} />
              <div className="mt-3 space-y-2">
                {matches.map((m) => (
                  <MatchRow key={m.id} match={m} />
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}

// ====== 하위 컴포넌트 ======

function DateHeader({ date }: { date: string }) {
  const d = new Date(`${date}T00:00:00`);
  const weekday = ["일", "월", "화", "수", "목", "금", "토"][d.getDay()];
  const label = `${d.getMonth() + 1}월 ${d.getDate()}일 (${weekday})`;
  return (
    <div className="sticky top-20 z-10 -mx-4 bg-gray-50 px-4 py-2 sm:-mx-6 sm:px-6">
      <h2 className="text-sm font-bold text-gray-700">{label}</h2>
    </div>
  );
}

function MatchRow({ match }: { match: ScheduledMatch }) {
  const time = formatTime(match.startAt);
  const badge = STATUS_BADGE[match.status];
  const finished = match.status === "FINISHED";

  return (
    <Card className="p-3 sm:p-4">
      {/* 모바일: 세로 / PC: 가로 */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        {/* 좌측: 시간 + 리그 뱃지 + BO */}
        <div className="flex w-full items-center justify-between gap-2 sm:w-44 sm:justify-start">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold">{time}</span>
            <LeagueBadge code={match.leagueCode} />
          </div>
          <span className="text-xs text-muted-foreground">BO{match.bestOf}</span>
        </div>

        {/* 중앙: 팀 vs 팀 */}
        <div className="flex flex-1 items-center justify-center gap-3 sm:gap-4">
          <TeamCell team={match.teamA} align="right" />
          <ScoreCell
            finished={finished}
            scoreA={match.scoreA}
            scoreB={match.scoreB}
            status={match.status}
          />
          <TeamCell team={match.teamB} align="left" />
        </div>

        {/* 우측: 상태 뱃지 */}
        <div className="flex justify-end sm:w-20">
          <span
            className={cn(
              "rounded px-2 py-0.5 text-xs font-semibold",
              badge.className,
            )}
          >
            {badge.label}
          </span>
        </div>
      </div>
    </Card>
  );
}

function LeagueBadge({ code }: { code: LeagueCode }) {
  return (
    <span className="rounded bg-gray-100 px-1.5 py-0.5 text-[10px] font-bold tracking-wide text-gray-700">
      {code}
    </span>
  );
}

function TeamCell({
  team,
  align,
}: {
  team: ScheduleTeam;
  align: "left" | "right";
}) {
  return (
    <div
      className={cn(
        "flex flex-1 items-center gap-2",
        align === "right" ? "flex-row-reverse text-right" : "flex-row",
      )}
    >
      <span
        className={cn(
          "flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[10px] font-bold text-white",
          team.color,
        )}
      >
        {team.code}
      </span>
      <span className="truncate text-sm font-medium">{team.name}</span>
    </div>
  );
}

function ScoreCell({
  finished,
  scoreA,
  scoreB,
  status,
}: {
  finished: boolean;
  scoreA?: number;
  scoreB?: number;
  status: MatchStatus;
}) {
  if (status === "LIVE" && scoreA !== undefined && scoreB !== undefined) {
    return (
      <div className="flex shrink-0 items-center gap-2 text-lg font-bold text-red-500">
        <span>{scoreA}</span>
        <span className="text-xs text-muted-foreground">:</span>
        <span>{scoreB}</span>
      </div>
    );
  }
  if (finished && scoreA !== undefined && scoreB !== undefined) {
    return (
      <div className="flex shrink-0 items-center gap-2 text-lg font-bold">
        <span className={scoreA > scoreB ? "text-foreground" : "text-gray-400"}>
          {scoreA}
        </span>
        <span className="text-xs text-muted-foreground">:</span>
        <span className={scoreB > scoreA ? "text-foreground" : "text-gray-400"}>
          {scoreB}
        </span>
      </div>
    );
  }
  return (
    <span className="shrink-0 text-sm font-bold text-muted-foreground">VS</span>
  );
}

