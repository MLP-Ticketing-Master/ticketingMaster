import { HeroSection } from "@/components/main/HeroSection";
import { GameFilter } from "@/components/main/GameFilter";
import { EventGrid } from "@/components/main/EventGrid";
import { useEventList } from "@/hooks";
import { useEventFilterStore } from "@/store";

export default function HomePage() {
  const game = useEventFilterStore((s) => s.game);
  const setGame = useEventFilterStore((s) => s.setGame);
  const { data = [], isLoading } = useEventList(game);

  return (
    <>
      <HeroSection />
      <section className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-12">
        <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-2xl font-bold">진행중인 대회</h2>
          <GameFilter value={game} onChange={setGame} />
        </div>
        {isLoading ? (
          <div className="py-20 text-center text-muted-foreground">
            불러오는 중...
          </div>
        ) : (
          <EventGrid events={data} />
        )}
      </section>
    </>
  );
}
