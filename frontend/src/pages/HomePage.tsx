import { HeroSection } from "@/components/event/HeroSection";
import { GameFilter } from "@/components/event/GameFilter";
import { EventGrid } from "@/components/event/EventGrid";
import { useEventList } from "@/hooks";
import { useEventFilterStore } from "@/store";

export default function HomePage() {
  const sportType = useEventFilterStore((s) => s.sportType);
  const setSportType = useEventFilterStore((s) => s.setSportType);
  const { data: events = [], isLoading, isError } = useEventList(sportType);

  return (
    <>
      <HeroSection />
      <section className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-12">
        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-2xl font-bold">진행중인 대회</h2>
          <GameFilter value={sportType} onChange={setSportType} />
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