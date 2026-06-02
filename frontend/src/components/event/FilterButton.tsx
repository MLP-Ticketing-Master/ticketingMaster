import { useState, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";
import { SPORT_FILTER_LABEL } from "@/lib/constants";
import type { SportType, EventStatus } from "@/types";

// ── 아이콘: 필터 슬라이더 (가로줄 + 노브) ───────────────────────
function FilterIcon({ active }: { active: boolean }) {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 20 20"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      {/* 세 줄 */}
      <line x1="2" y1="5" x2="18" y2="5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <line x1="2" y1="10" x2="18" y2="10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <line x1="2" y1="15" x2="18" y2="15" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      {/* 노브 */}
      <circle cx="6" cy="5" r="2.2" fill={active ? "#316DFD" : "white"} stroke="currentColor" strokeWidth="1.6" />
      <circle cx="13" cy="10" r="2.2" fill={active ? "#316DFD" : "white"} stroke="currentColor" strokeWidth="1.6" />
      <circle cx="8" cy="15" r="2.2" fill={active ? "#316DFD" : "white"} stroke="currentColor" strokeWidth="1.6" />
    </svg>
  );
}

// ── 캘린더 미니 컴포넌트 ─────────────────────────────────────────
interface CalendarProps {
  selectedFrom: string | null;
  selectedTo: string | null;
  onChange: (from: string | null, to: string | null) => void;
}

function MiniCalendar({ selectedFrom, selectedTo, onChange }: CalendarProps) {
  const today = new Date();
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth()); // 0-based

  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

  // 달력 날짜 계산
  const firstDay = new Date(viewYear, viewMonth, 1).getDay(); // 0=일
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();

  const cells: (number | null)[] = [
    ...Array(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];
  // 6행 맞추기
  while (cells.length % 7 !== 0) cells.push(null);

  function dateStr(day: number) {
    return `${viewYear}-${String(viewMonth + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  function handleDayClick(day: number) {
    const d = dateStr(day);
    if (!selectedFrom || (selectedFrom && selectedTo)) {
      // 새로 시작
      onChange(d, null);
    } else {
      // from 이미 있음 → to 세팅
      if (d < selectedFrom) {
        onChange(d, selectedFrom);
      } else if (d === selectedFrom) {
        onChange(null, null);
      } else {
        onChange(selectedFrom, d);
      }
    }
  }

  function isInRange(day: number) {
    if (!selectedFrom || !selectedTo) return false;
    const d = dateStr(day);
    return d > selectedFrom && d < selectedTo;
  }
  function isFrom(day: number) { return dateStr(day) === selectedFrom; }
  function isTo(day: number) { return dateStr(day) === selectedTo; }
  function isToday(day: number) { return dateStr(day) === todayStr; }

  const prevMonth = () => {
    if (viewMonth === 0) { setViewYear(y => y - 1); setViewMonth(11); }
    else setViewMonth(m => m - 1);
  };
  const nextMonth = () => {
    if (viewMonth === 11) { setViewYear(y => y + 1); setViewMonth(0); }
    else setViewMonth(m => m + 1);
  };

  const monthLabel = `${viewYear}.${String(viewMonth + 1).padStart(2, "0")}`;
  const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

  return (
    <div className="select-none">
      {/* 헤더 */}
      <div className="mb-3 flex items-center justify-between">
        <button
          type="button"
          onClick={prevMonth}
          className="rounded p-1 text-muted-foreground hover:bg-gray-100"
          aria-label="이전 달"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M10 12L6 8L10 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        <span className="text-sm font-bold">{monthLabel}</span>
        <button
          type="button"
          onClick={nextMonth}
          className="rounded p-1 text-muted-foreground hover:bg-gray-100"
          aria-label="다음 달"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 12L10 8L6 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>

      {/* 요일 헤더 */}
      <div className="mb-1 grid grid-cols-7 text-center">
        {WEEKDAYS.map((w) => (
          <div key={w} className="py-1 text-[11px] font-medium text-muted-foreground">{w}</div>
        ))}
      </div>

      {/* 날짜 그리드 */}
      <div className="grid grid-cols-7">
        {cells.map((day, idx) => {
          if (!day) return <div key={idx} />;
          const from = isFrom(day);
          const to = isTo(day);
          const inRange = isInRange(day);
          const tod = isToday(day);
          return (
            <div key={idx} className="relative flex justify-center py-0.5">
              {/* 범위 배경 */}
              {inRange && (
                <div className="absolute inset-y-0.5 left-0 right-0 bg-blue-50" />
              )}
              {/* from/to 반원 배경 */}
              {(from || to) && selectedFrom && selectedTo && (
                <div
                  className={cn(
                    "absolute inset-y-0.5 w-1/2 bg-blue-50",
                    from ? "right-0" : "left-0",
                  )}
                />
              )}
              <button
                type="button"
                onClick={() => handleDayClick(day)}
                className={cn(
                  "relative z-10 flex h-8 w-8 items-center justify-center rounded-full text-xs font-medium transition-colors",
                  (from || to) && "bg-[#316DFD] text-white",
                  !from && !to && tod && "font-bold text-[#316DFD]",
                  !from && !to && inRange && "text-[#316DFD]",
                  !from && !to && !inRange && !tod && "text-gray-700 hover:bg-gray-100",
                )}
              >
                {day}
                {tod && !from && !to && (
                  <span className="absolute bottom-0.5 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-[#316DFD]" />
                )}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── 필터 모달 ────────────────────────────────────────────────────
const SPORT_FILTERS: SportType[] = ["ALL", "LOL", "VALORANT", "OVERWATCH", "TFT", "PUBG"];
const STATUS_OPTIONS: { key: EventStatus; label: string }[] = [
  { key: "OPEN", label: "진행 중" },
  { key: "UPCOMING", label: "진행예정" },
  { key: "FINISHED", label: "진행종료" },
];

interface FilterModalProps {
  sportType: SportType;
  statuses: EventStatus[];
  dateFrom: string | null;
  dateTo: string | null;
  onSportChange: (v: SportType) => void;
  onStatusToggle: (v: EventStatus) => void;
  onDateChange: (from: string | null, to: string | null) => void;
  onReset: () => void;
  onApply: () => void;
  resultCount?: number;
}

function FilterModal({
  sportType,
  statuses,
  dateFrom,
  dateTo,
  onSportChange,
  onStatusToggle,
  onDateChange,
  onReset,
  onApply,
  resultCount,
}: FilterModalProps) {
  function formatDateLabel(d: string) {
    // "YYYY-MM-DD" → "M월 D일"
    const [, m, day] = d.split("-");
    return `${parseInt(m)}월 ${parseInt(day)}일`;
  }

  return (
    <div className="flex flex-col gap-6">
      {/* 종목 */}
      <section>
        <h3 className="mb-3 text-sm font-semibold text-gray-700">장르</h3>
        <div className="flex flex-wrap gap-2">
          {SPORT_FILTERS.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => onSportChange(s)}
              className={cn(
                "rounded-full border px-4 py-1.5 text-sm font-medium transition-colors",
                sportType === s
                  ? "border-[#316DFD] bg-[#316DFD] text-white"
                  : "border-gray-200 bg-white text-gray-700 hover:border-gray-300",
              )}
            >
              {SPORT_FILTER_LABEL[s]}
            </button>
          ))}
        </div>
      </section>

      <div className="h-px bg-gray-100" />

      {/* 진행 상태 (다중 선택) */}
      <section>
        <h3 className="mb-3 text-sm font-semibold text-gray-700">
          진행 상태
          <span className="ml-1.5 text-xs font-normal text-muted-foreground">(중복 선택 가능)</span>
        </h3>
        <div className="flex flex-wrap gap-2">
          {STATUS_OPTIONS.map((opt) => {
            const active = statuses.includes(opt.key);
            return (
              <button
                key={opt.key}
                type="button"
                onClick={() => onStatusToggle(opt.key)}
                className={cn(
                  "rounded-full border px-4 py-1.5 text-sm font-medium transition-colors",
                  active
                    ? "border-[#316DFD] bg-[#316DFD] text-white"
                    : "border-gray-200 bg-white text-gray-700 hover:border-gray-300",
                )}
              >
                {opt.label}
              </button>
            );
          })}
        </div>
      </section>

      <div className="h-px bg-gray-100" />

      {/* 날짜 */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-gray-700">날짜</h3>
          {(dateFrom || dateTo) && (
            <span className="text-xs text-[#316DFD]">
              {dateFrom ? formatDateLabel(dateFrom) : "—"}
              {dateTo ? ` ~ ${formatDateLabel(dateTo)}` : " ~"}
            </span>
          )}
        </div>
        <MiniCalendar
          selectedFrom={dateFrom}
          selectedTo={dateTo}
          onChange={onDateChange}
        />
      </section>

      {/* 하단 버튼 */}
      <div className="flex gap-2 pt-1">
        <button
          type="button"
          onClick={onReset}
          className="flex-1 rounded-xl border border-gray-200 py-3 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50"
        >
          초기화
        </button>
        <button
          type="button"
          onClick={onApply}
          className="flex-[2] rounded-xl bg-[#316DFD] py-3 text-sm font-semibold text-white transition-colors hover:bg-[#1C5EFD]"
        >
          {resultCount !== undefined ? `${resultCount}개 검색` : "검색"}
        </button>
      </div>
    </div>
  );
}

// ── 메인 export: 필터 버튼 + 모달 ────────────────────────────────
interface FilterButtonProps {
  sportType: SportType;
  statuses: EventStatus[];
  dateFrom: string | null;
  dateTo: string | null;
  hasActiveFilters: boolean;
  onSportChange: (v: SportType) => void;
  onStatusToggle: (v: EventStatus) => void;
  onDateChange: (from: string | null, to: string | null) => void;
  onReset: () => void;
  resultCount?: number;
}

export function FilterButton({
  sportType,
  statuses,
  dateFrom,
  dateTo,
  hasActiveFilters,
  onSportChange,
  onStatusToggle,
  onDateChange,
  onReset,
  resultCount,
}: FilterButtonProps) {
  const [open, setOpen] = useState(false);
  const overlayRef = useRef<HTMLDivElement>(null);

  // 바깥 클릭 → 닫기
  useEffect(() => {
    if (!open) return;
    function handleClick(e: MouseEvent) {
      if (overlayRef.current && !overlayRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  // 스크롤 잠금
  useEffect(() => {
    document.body.style.overflow = open ? "hidden" : "";
    return () => { document.body.style.overflow = ""; };
  }, [open]);

  return (
    <>
      {/* 필터 트리거 버튼 */}
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="필터"
        className={cn(
          "flex items-center gap-1.5 rounded-full border px-3 py-2 text-sm font-medium transition-all",
          hasActiveFilters
            ? "border-[#316DFD] bg-[#EEF3FF] text-[#316DFD]"
            : "border-gray-200 bg-white text-gray-600 hover:border-gray-300 hover:bg-gray-50",
        )}
      >
        <FilterIcon active={hasActiveFilters} />
        <span className="hidden sm:inline">필터</span>
        {hasActiveFilters && (
          <span className="flex h-4 w-4 items-center justify-center rounded-full bg-[#316DFD] text-[10px] font-bold text-white">
            {[sportType !== "ALL", statuses.length > 0, dateFrom !== null].filter(Boolean).length}
          </span>
        )}
      </button>

      {/* 모달 오버레이 */}
      {open && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center">
          <div
            ref={overlayRef}
            className="relative w-full max-w-md overflow-hidden rounded-t-2xl bg-white shadow-2xl sm:rounded-2xl"
            style={{ maxHeight: "90dvh" }}
          >
            {/* 모달 헤더 */}
            <div className="flex items-center justify-between border-b px-5 py-4">
              <span className="text-base font-bold">필터</span>
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="닫기"
                className="rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
              >
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path d="M15 5L5 15M5 5L15 15" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                </svg>
              </button>
            </div>

            {/* 스크롤 가능한 콘텐츠 */}
            <div className="overflow-y-auto px-5 py-5" style={{ maxHeight: "calc(90dvh - 64px)" }}>
              <FilterModal
                sportType={sportType}
                statuses={statuses}
                dateFrom={dateFrom}
                dateTo={dateTo}
                onSportChange={onSportChange}
                onStatusToggle={onStatusToggle}
                onDateChange={onDateChange}
                onReset={onReset}
                onApply={() => setOpen(false)}
                resultCount={resultCount}
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
