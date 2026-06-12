import { useMemo, useState } from "react";
import { HeroSection } from "@/components/event/HeroSection";
import { FilterButton } from "@/components/event/FilterButton";
import { EventGrid } from "@/components/event/EventGrid";
import { useEventList } from "@/hooks";
import { useEventFilterStore } from "@/store";

export default function HomePage() {
  const sportType = useEventFilterStore((s) => s.sportType);
  const statuses = useEventFilterStore((s) => s.statuses);
  const dateFrom = useEventFilterStore((s) => s.dateFrom);
  const dateTo = useEventFilterStore((s) => s.dateTo);
  const hasActiveFilters = useEventFilterStore((s) => s.hasActiveFilters);

  const setSportType = useEventFilterStore((s) => s.setSportType);
  const toggleStatus = useEventFilterStore((s) => s.toggleStatus);
  const setDateRange = useEventFilterStore((s) => s.setDateRange);
  const reset = useEventFilterStore((s) => s.reset);

  const [currentPage, setCurrentPage] = useState(1);

  // 필터 변경 시 첫 페이지로 리셋하는 래퍼
  const handleSportChange: typeof setSportType = (value) => {
    setSportType(value);
    setCurrentPage(1);
  };
  const handleStatusToggle: typeof toggleStatus = (status) => {
    toggleStatus(status);
    setCurrentPage(1);
  };
  const handleDateChange: typeof setDateRange = (from, to) => {
    setDateRange(from, to);
    setCurrentPage(1);
  };
  const handleReset = () => {
    reset();
    setCurrentPage(1);
  };

  const { data: rawEvents = [], isLoading, isError } = useEventList(sportType);

  const events = useMemo(() => {
    let filtered = rawEvents;

    // 상태 필터: 선택된 게 없으면 디폴트로 OPEN(진행 중)만 표시
    const activeStatuses = statuses.length > 0 ? statuses : ["OPEN" as const];
    filtered = filtered.filter((e) => activeStatuses.includes(e.status));

    // 날짜 범위 필터
    if (dateFrom) {
      filtered = filtered.filter((e) => e.endDate >= dateFrom);
    }
    if (dateTo) {
      filtered = filtered.filter((e) => e.startDate <= dateTo);
    }

    // 이벤트 시작일 순 정렬
    return [...filtered].sort(
      (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime(),
    );
  }, [rawEvents, statuses, dateFrom, dateTo]);

  return (
    <>
      <HeroSection />
      <section className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-12">
        {/* 타이틀 + 필터 헤더 */}
        <div className="mb-8 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            {/* 강조 바 */}
            <div className="h-8 w-1.5 rounded-full bg-gradient-to-b from-[#054EFD] to-[#9B5FFC]" />
            <div>
              <h2 className="bg-gradient-to-r from-[#054EFD] to-[#9B5FFC] bg-clip-text text-2xl font-extrabold tracking-tight text-transparent sm:text-3xl">
                Esports
              </h2>
            </div>
          </div>
          <FilterButton
            sportType={sportType}
            statuses={statuses}
            dateFrom={dateFrom}
            dateTo={dateTo}
            hasActiveFilters={hasActiveFilters}
            onSportChange={handleSportChange}
            onStatusToggle={handleStatusToggle}
            onDateChange={handleDateChange}
            onReset={handleReset}
            resultCount={isLoading ? undefined : events.length}
          />
        </div>

        {isLoading ? (
          <div className="py-20 text-center text-muted-foreground">
            불러오는 중...
          </div>
        ) : isError ? (
          <div className="py-20 text-center text-red-500">
            이벤트를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
          </div>
        ) : (
          <EventGrid
            events={events}
            currentPage={currentPage}
            onPageChange={setCurrentPage}
          />
        )}
      </section>
    </>
  );
}
