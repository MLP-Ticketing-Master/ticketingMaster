import { useMemo, useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { AdminCard } from "@/components/admin/AdminCard";
import {
  useEventList,
  useRounds,
  useSeatGrades,
  useSeatLayout,
  useSections,
} from "@/hooks";
import {
  SEAT_GRADE_BG_SOFT,
  SEAT_GRADE_COLORS,
} from "@/lib/constants";
import { formatPrice, formatDate, formatTime } from "@/lib/format";
import type { Seat } from "@/types";

export default function SeatsAdminPage() {
  const { data: events = [] } = useEventList("ALL");
  const [eventId, setEventId] = useState<number | null>(null);

  const currentEventId = eventId ?? events[0]?.id ?? null;

  return (
    <div className="space-y-6">
      <SeatGradeSection
        events={events}
        eventId={currentEventId}
        onChangeEvent={setEventId}
      />
      {currentEventId && <SectionsSection eventId={currentEventId} />}
      {currentEventId && <RoundSeatStatusSection eventId={currentEventId} />}
    </div>
  );
}

function SeatGradeSection({
  events,
  eventId,
  onChangeEvent,
}: {
  events: { id: number; title: string }[];
  eventId: number | null;
  onChangeEvent: (id: number) => void;
}) {
  const { data: grades = [] } = useSeatGrades(eventId ?? 0);

  return (
    <AdminCard title="좌석 등급 관리">
      <Select
        value={eventId ? String(eventId) : ""}
        onValueChange={(v) => onChangeEvent(Number(v))}
      >
        <SelectTrigger className="w-full max-w-md">
          <SelectValue placeholder="대회를 선택하세요" />
        </SelectTrigger>
        <SelectContent>
          {events.map((e) => (
            <SelectItem key={e.id} value={String(e.id)}>
              {e.title}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <ul className="mt-5 space-y-3">
        {grades.map((g) => (
          <li
            key={g.code}
            className="flex items-center justify-between rounded-lg border bg-white px-5 py-4"
          >
            <div className="flex items-center gap-4">
              <span
                className={`h-6 w-6 rounded ${SEAT_GRADE_COLORS[g.code]}`}
              />
              <div>
                <p className="font-bold">{g.name}</p>
                <p className="text-xs text-muted-foreground">
                  등급 코드: {g.code}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="text-right">
                <p className="font-bold">{formatPrice(g.price)}</p>
                <p className="text-xs text-muted-foreground">
                  정렬순서: {g.sortOrder}
                </p>
              </div>
              <Button size="sm" variant="outline">
                수정
              </Button>
            </div>
          </li>
        ))}
      </ul>
    </AdminCard>
  );
}

function SectionsSection({ eventId }: { eventId: number }) {
  const { data: sections = [] } = useSections(eventId);

  return (
    <AdminCard title="구역 관리">
      <div className="grid gap-4 md:grid-cols-3">
        {sections.map((s) => (
          <Card key={s.id} className="space-y-2 p-5">
            <p className="font-bold">{s.name}</p>
            <p className="text-xs text-muted-foreground">
              정렬순서: {s.sortOrder}
            </p>
            <p className="text-sm">{s.description}</p>
            <Button variant="outline" className="w-full">
              수정
            </Button>
          </Card>
        ))}
      </div>
    </AdminCard>
  );
}

function RoundSeatStatusSection({ eventId }: { eventId: number }) {
  const { data: rounds = [] } = useRounds(eventId);
  const [roundId, setRoundId] = useState<number | null>(null);
  const currentRoundId = roundId ?? rounds[0]?.id ?? null;
  const { data: grades = [] } = useSeatGrades(eventId);
  const { data: layout } = useSeatLayout(currentRoundId ?? 0, 1);

  const totals = useMemo(() => {
    const seats = layout?.seats ?? [];
    return {
      total: seats.length,
      sold: seats.filter((s) => s.status === "SOLD").length,
      available: seats.filter((s) => s.status === "AVAILABLE").length,
    };
  }, [layout]);

  const round = rounds.find((r) => r.id === currentRoundId);
  const seatsByRow = (layout?.seats ?? []).reduce<Record<string, Seat[]>>(
    (acc, s) => {
      (acc[s.row] ??= []).push(s);
      return acc;
    },
    {},
  );

  return (
    <AdminCard
      title="회차별 좌석 현황"
      action={
        <Select
          value={currentRoundId ? String(currentRoundId) : ""}
          onValueChange={(v) => setRoundId(Number(v))}
        >
          <SelectTrigger className="w-72">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {rounds.map((r) => (
              <SelectItem key={r.id} value={String(r.id)}>
                {formatDate(r.startAt)} {formatTime(r.startAt)} ({r.matchTitle})
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      }
    >
      <div className="space-y-5">
        {round && (
          <p className="text-sm text-muted-foreground">대회: {round.matchTitle}</p>
        )}

        <div className="space-y-2">
          <p className="font-semibold">좌석 등급 범례</p>
          <ul className="flex flex-wrap gap-4 text-sm">
            {grades.map((g) => (
              <li key={g.code} className="flex items-center gap-2">
                <span
                  className={`h-3 w-3 rounded-sm ${SEAT_GRADE_COLORS[g.code]}`}
                />
                {g.name} ({formatPrice(g.price)})
              </li>
            ))}
          </ul>
        </div>

        <div className="flex justify-center">
          <Button
            variant="secondary"
            className="rounded-full text-xs text-muted-foreground"
          >
            MAIN SCREEN
          </Button>
        </div>

        <div className="rounded-xl border bg-white p-6">
          <div className="space-y-2">
            {layout?.rows.map((row) => (
              <div key={row} className="flex items-center gap-2">
                <span className="w-5 text-sm font-medium text-muted-foreground">
                  {row}
                </span>
                <div className="flex flex-1 justify-center gap-1.5">
                  {(seatsByRow[row] ?? []).map((seat) => (
                    <span
                      key={seat.id}
                      className={`h-5 w-5 rounded ${
                        seat.status === "SOLD"
                          ? SEAT_GRADE_BG_SOFT[seat.gradeCode]
                          : SEAT_GRADE_COLORS[seat.gradeCode]
                      }`}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <SummaryCard label="전체 좌석" value={totals.total} tone="rose" />
          <SummaryCard
            label="판매 가능"
            value={totals.available}
            tone="green"
          />
          <SummaryCard label="판매 완료" value={totals.sold} tone="rose" />
        </div>
      </div>
    </AdminCard>
  );
}

function SummaryCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: "rose" | "green";
}) {
  const cls =
    tone === "green"
      ? "bg-green-50 text-green-600"
      : "bg-rose-50 text-rose-500";
  return (
    <Card className={`${cls.split(" ")[0]} p-5 text-center`}>
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${cls.split(" ")[1]}`}>{value}</p>
    </Card>
  );
}
