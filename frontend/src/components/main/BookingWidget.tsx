import { useMemo, useState } from "react";
import { Calendar, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { GAME_LABEL } from "@/lib/constants";
import { formatShortDate, formatTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { EventDetail, Round } from "@/types";

interface Props {
  event: EventDetail;
  onProceed: (roundId: number) => void;
}

export function BookingWidget({ event, onProceed }: Props) {
  const dates = useMemo(() => {
    const set = new Set(event.rounds.map((r) => r.startAt.slice(0, 10)));
    return Array.from(set);
  }, [event.rounds]);

  const [selectedDate, setSelectedDate] = useState<string>(dates[0] ?? "");
  const [selectedRoundId, setSelectedRoundId] = useState<number | null>(
    event.rounds[0]?.id ?? null,
  );

  const roundsOfDate = event.rounds.filter((r) =>
    r.startAt.startsWith(selectedDate),
  );

  const game = event.game === "ALL" ? "LOL" : event.game;

  return (
    <Card className="space-y-5 p-6">
      <div className="space-y-2">
        <Badge
          variant="secondary"
          className="bg-blue-100 text-[#3C76FE]"
        >
          {GAME_LABEL[game]}
        </Badge>
        <h2 className="text-xl font-bold leading-snug">{event.title}</h2>
        <p className="text-sm text-muted-foreground">{event.venue}</p>
      </div>

      <div className="space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold">
          <Calendar className="h-4 w-4" />
          관람일 선택
        </div>
        <div className="flex flex-wrap gap-2">
          {dates.map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => setSelectedDate(d)}
              className={cn(
                "rounded-lg border px-4 py-2 text-sm font-medium transition-colors",
                selectedDate === d
                  ? "border-[#054EFD] bg-blue-50 text-[#3C76FE]"
                  : "border-gray-200 bg-white hover:bg-gray-50",
              )}
            >
              {formatShortDate(d)}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-3">
        <div className="text-sm font-semibold">회차 선택</div>
        <div className="space-y-2">
          {roundsOfDate.map((round) => (
            <RoundButton
              key={round.id}
              round={round}
              selected={selectedRoundId === round.id}
              onSelect={() => setSelectedRoundId(round.id)}
            />
          ))}
          {roundsOfDate.length === 0 && (
            <p className="text-sm text-muted-foreground">
              해당 날짜의 회차가 없습니다.
            </p>
          )}
        </div>
      </div>

      <Button
        size="lg"
        disabled={!selectedRoundId}
        onClick={() => selectedRoundId && onProceed(selectedRoundId)}
        className="w-full bg-[#054EFD] hover:bg-[#3C76FE] disabled:bg-gray-200 disabled:text-gray-400"
      >
        예매하기
        <ChevronRight className="ml-1 h-4 w-4" />
      </Button>
    </Card>
  );
}

function RoundButton({
  round,
  selected,
  onSelect,
}: {
  round: Round;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={cn(
        "flex w-full items-center justify-between rounded-lg border px-4 py-3 text-sm font-medium transition-colors",
        selected
          ? "border-[#3C76FE] bg-blue-50"
          : "border-gray-200 bg-white hover:bg-gray-50",
      )}
    >
      <span>{formatTime(round.startAt)}</span>
      <span className="text-xs text-muted-foreground">예매가능</span>
    </button>
  );
}
