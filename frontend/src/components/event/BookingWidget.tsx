import { useMemo, useState } from "react";
import { Calendar, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { SPORT_LABEL } from "@/lib/constants";
import { formatShortDate, formatTime, formatPrice, normalizeColorHex } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { EventDetailResponse, MatchResponse } from "@/types";

interface Props {
  event: EventDetailResponse;
  onProceed: (matchId: number) => void;
}

export function BookingWidget({ event, onProceed }: Props) {
  const dates = useMemo(() => {
    const set = new Set(event.matches.map((m) => m.matchDate));
    return Array.from(set).sort();
  }, [event.matches]);

  const [selectedDate, setSelectedDate] = useState<string>(dates[0] ?? "");

  // 초기 선택은 첫 "예매 가능" 매치 — 없으면 첫 매치로 fallback
  const initialMatchId =
    event.matches.find((m) => m.bookable ?? m.isBookable)?.matchId ??
    event.matches[0]?.matchId ??
    null;
  const [selectedMatchId, setSelectedMatchId] = useState<number | null>(
    initialMatchId,
  );

  const matchesOfDate = event.matches.filter(
    (m) => m.matchDate === selectedDate,
  );

  const selectedMatch = event.matches.find(
    (m) => m.matchId === selectedMatchId,
  );
  const canBookSelected =
    (selectedMatch?.bookable ?? selectedMatch?.isBookable) === true;

  // 가격 내림차순 정렬 (VIP → R → S → A 순)
  const sortedGrades = useMemo(
    () => [...event.seatGrades].sort((a, b) => b.price - a.price),
    [event.seatGrades],
  );

  const sportLabel =
    event.sportType in SPORT_LABEL
      ? SPORT_LABEL[event.sportType as keyof typeof SPORT_LABEL]
      : event.sportType;

  return (
    <div className="space-y-4">
      <Card className="space-y-5 p-6">
        {/* 이벤트 헤더 */}
        <div className="space-y-2">
          <Badge variant="secondary" className="bg-blue-100 text-[#3C76FE]">
            {sportLabel}
          </Badge>
          <h2 className="text-xl font-bold leading-snug">{event.title}</h2>
          <p className="text-sm text-muted-foreground">{event.place}</p>
        </div>

        {/* 날짜 선택 */}
        {dates.length > 0 && (
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
                  onClick={() => {
                    setSelectedDate(d);
                    const first = event.matches.find((m) => m.matchDate === d);
                    setSelectedMatchId(first?.matchId ?? null);
                  }}
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
        )}

        {/* 회차 선택 */}
        <div className="space-y-3">
          <div className="text-sm font-semibold">회차 선택</div>
          <div className="space-y-2">
            {matchesOfDate.map((match) => (
              <MatchButton
                key={match.matchId}
                match={match}
                selected={selectedMatchId === match.matchId}
                onSelect={() => setSelectedMatchId(match.matchId)}
              />
            ))}
            {matchesOfDate.length === 0 && (
              <p className="text-sm text-muted-foreground">
                해당 날짜의 회차가 없습니다.
              </p>
            )}
          </div>
        </div>

        {/* 티켓 가격 — 가격 내림차순 */}
        {sortedGrades.length > 0 && (
          <div className="space-y-2">
            <div className="text-sm font-semibold">티켓 가격</div>
            <ul className="space-y-1.5">
              {sortedGrades.map((grade) => (
                <li
                  key={grade.seatGradeId}
                  className="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2"
                >
                  <div className="flex items-center gap-2">
                    <span
                      className="h-4 w-4 rounded-sm"
                      style={{ backgroundColor: normalizeColorHex(grade.colorHex) }}
                    />
                    <span className="text-sm font-semibold">{grade.gradeCode}석</span>
                  </div>
                  <span className="text-sm font-bold">{formatPrice(grade.price)}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* 예매하기 버튼 — 선택된 회차가 예매 불가면 비활성 */}
        <Button
          size="lg"
          disabled={!selectedMatchId || !canBookSelected}
          onClick={() => selectedMatchId && onProceed(selectedMatchId)}
          className="w-full bg-[#054EFD] hover:bg-[#3C76FE] disabled:bg-gray-200 disabled:text-gray-400"
        >
          {canBookSelected ? "예매하기" : "예매 불가"}
          <ChevronRight className="ml-1 h-4 w-4" />
        </Button>
      </Card>

      {/* 예매 안내 — 별도 카드 */}
      {event.bookingNotice && (
        <div className="rounded-xl bg-blue-50 px-5 py-4">
          <p className="mb-3 text-sm font-bold text-blue-700">예매 안내</p>
          <ul className="space-y-1.5">
            {event.bookingNotice
              .split("\n")
              .map((line) => line.trim())
              .filter(Boolean)
              .map((line, i) => (
                <li key={i} className="flex items-start gap-2 text-sm text-blue-900">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
                  {line}
                </li>
              ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function MatchButton({
  match,
  selected,
  onSelect,
}: {
  match: MatchResponse;
  selected: boolean;
  onSelect: () => void;
}) {
  const bookable = match.bookable ?? match.isBookable ?? false;

  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={!bookable}
      className={cn(
        "flex w-full items-center justify-between rounded-lg border px-4 py-3 text-sm font-medium transition-colors",
        selected
          ? "border-[#3C76FE] bg-blue-50"
          : bookable
            ? "border-gray-200 bg-white hover:bg-gray-50"
            : "cursor-not-allowed border-gray-100 bg-gray-50 opacity-50",
      )}
    >
      <div className="flex flex-col items-start gap-0.5">
        {match.roundLabel && (
          <span className="text-xs text-muted-foreground">{match.roundLabel}</span>
        )}
        <span>{formatTime(match.startAt)}</span>
      </div>
      <span
        className={cn(
          "text-xs font-semibold",
          bookable ? "text-green-600" : "text-gray-400",
        )}
      >
        {bookable ? "예매가능" : "예매불가"}
      </span>
    </button>
  );
}