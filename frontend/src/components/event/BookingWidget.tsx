import { useMemo, useState } from "react";
import { Calendar, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { SPORT_LABEL } from "@/lib/constants";
import { formatShortDate, formatTime, formatPrice, normalizeColorHex } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { EventDetailResponse, MatchResponse } from "@/types";

/** 매치의 예매 상태를 3가지로 분류 */
type BookingStatus = "open" | "upcoming" | "closed";

function getBookingStatus(match: MatchResponse): BookingStatus {
  const now = new Date();
  const openAt = new Date(match.bookingOpenAt);
  const closeAt = new Date(match.bookingCloseAt);

  if (now < openAt) return "upcoming";
  if (now > closeAt) return "closed";
  return "open";
}

/** "6/5 17:00 오픈예정" 형태의 짧은 날짜 포맷 */
function formatOpenAt(iso: string): string {
  const d = new Date(iso);
  const month = d.getMonth() + 1;
  const day = d.getDate();
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${month}/${day} ${hh}:${mm} 오픈예정`;
}

interface Props {
  event: EventDetailResponse;
  onProceed: (matchId: number) => void;
}

export function BookingWidget({ event, onProceed }: Props) {
  // 예매 가능 또는 예매 예정 매치만 표시 (종료된 매치 완전 제외)
  const visibleMatches = useMemo(
    () => event.matches.filter((m) => getBookingStatus(m) !== "closed"),
    [event.matches],
  );

  const dates = useMemo(() => {
    const set = new Set(visibleMatches.map((m) => m.matchDate));
    return Array.from(set).sort();
  }, [visibleMatches]);

  // 초기 선택은 첫 "예매 가능(open)" 매치 — 없으면 첫 "예매 예정(upcoming)"
  const initialMatch =
    visibleMatches.find((m) => getBookingStatus(m) === "open") ??
    visibleMatches.find((m) => getBookingStatus(m) === "upcoming") ??
    visibleMatches[0] ??
    null;

  const [selectedDate, setSelectedDate] = useState<string>(
    initialMatch?.matchDate ?? dates[0] ?? "",
  );
  const [selectedMatchId, setSelectedMatchId] = useState<number | null>(
    initialMatch?.matchId ?? null,
  );

  const matchesOfDate = visibleMatches.filter(
    (m) => m.matchDate === selectedDate,
  );

  const selectedMatch = visibleMatches.find((m) => m.matchId === selectedMatchId);
  const selectedBookingStatus = selectedMatch ? getBookingStatus(selectedMatch) : null;
  const canBookSelected = selectedBookingStatus === "open";

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
                    // 해당 날짜의 예매 가능(open) 회차 우선 → 예매예정(upcoming) → 첫 회차
                    const firstOfDate =
                      visibleMatches.find(
                        (m) => m.matchDate === d && getBookingStatus(m) === "open",
                      ) ??
                      visibleMatches.find(
                        (m) => m.matchDate === d && getBookingStatus(m) === "upcoming",
                      ) ??
                      visibleMatches.find((m) => m.matchDate === d);
                    setSelectedMatchId(firstOfDate?.matchId ?? null);
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
            {matchesOfDate.length === 0 && dates.length === 0 && (
              <p className="text-sm text-muted-foreground">
                예매 가능한 회차가 없습니다.
              </p>
            )}
            {matchesOfDate.length === 0 && dates.length > 0 && (
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

        {/* 예매하기 버튼 — 상태별 텍스트 및 비활성 처리 */}
        <Button
          size="lg"
          disabled={!selectedMatchId || !canBookSelected}
          onClick={() => selectedMatchId && onProceed(selectedMatchId)}
          className={cn(
            "w-full disabled:text-gray-400",
            selectedBookingStatus === "upcoming"
              ? "bg-amber-400 hover:bg-amber-500 disabled:bg-amber-100"
              : "bg-[#054EFD] hover:bg-[#3C76FE] disabled:bg-gray-200",
          )}
        >
          {selectedBookingStatus === "open" && "예매하기"}
          {selectedBookingStatus === "upcoming" && "오픈 예정"}
          {selectedBookingStatus === "closed" && "예매 종료"}
          {!selectedMatchId && "회차를 선택하세요"}
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
  const status = getBookingStatus(match);
  const isOpen = status === "open";
  const isUpcoming = status === "upcoming";
  const isClosed = status === "closed";

  return (
    <button
      type="button"
      onClick={isOpen || isUpcoming ? onSelect : undefined}
      disabled={isClosed}
      className={cn(
        "flex w-full items-center justify-between rounded-lg border px-4 py-3 text-sm font-medium transition-colors",
        isClosed && "cursor-not-allowed border-gray-100 bg-gray-100 opacity-60",
        isUpcoming && !selected && "border-amber-200 bg-amber-50 hover:bg-amber-100",
        isUpcoming && selected && "border-amber-400 bg-amber-100",
        isOpen && !selected && "border-gray-200 bg-white hover:bg-gray-50",
        isOpen && selected && "border-[#3C76FE] bg-blue-50",
      )}
    >
      <div className="flex flex-col items-start gap-0.5">
        {match.roundLabel && (
          <span className={cn("text-xs", isClosed ? "text-gray-400" : "text-muted-foreground")}>
            {match.roundLabel}
          </span>
        )}
        <span className={cn(isClosed && "text-gray-400")}>{formatTime(match.startAt)}</span>
      </div>

      {isClosed && (
        <span className="rounded-md bg-gray-200 px-2 py-0.5 text-xs font-semibold text-gray-500">
          종료
        </span>
      )}
      {isUpcoming && (
        <span className="text-right text-xs font-semibold text-amber-600">
          {formatOpenAt(match.bookingOpenAt)}
        </span>
      )}
      {isOpen && (
        <span className="text-xs font-semibold text-green-600">예매가능</span>
      )}
    </button>
  );
}