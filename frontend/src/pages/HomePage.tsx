import { useMemo } from "react";
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

  // sportType만 API로 필터링 (백엔드 지원), 나머지는 프론트에서 필터
  const { data: rawEvents = [], isLoading, isError } = useEventList(sportType);

  const events = useMemo(() => {
    let filtered = rawEvents;

    // 진행 상태 필터 (다중 선택 — 하나라도 선택 시 해당 상태만)
    if (statuses.length > 0) {
      filtered = filtered.filter((e) => statuses.includes(e.status));
    }

    // 날짜 범위 필터 (이벤트 기간이 선택 범위와 겹치는 것)
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
        <div className="mb-6 flex items-center justify-between gap-3">
          <h2 className="text-2xl font-bold">진행중인 대회</h2>
          <FilterButton
            sportType={sportType}
            statuses={statuses}
            dateFrom={dateFrom}
            dateTo={dateTo}
            hasActiveFilters={hasActiveFilters}
            onSportChange={setSportType}
            onStatusToggle={toggleStatus}
            onDateChange={setDateRange}
            onReset={reset}
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
          <EventGrid events={events} />
        )}
      </section>
    </>
  );
}
