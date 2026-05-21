import { useBookingFlowStore } from "@/store";
import { useSeatLayout } from "@/hooks";
import { SeatGrid } from "../SeatGrid";

export function SeatStep() {
  const matchId = useBookingFlowStore((s) => s.matchId);
  const sectionId = useBookingFlowStore((s) => s.sectionId);
  const selectedSeats = useBookingFlowStore((s) => s.selectedSeats);
  const goBackToZone = useBookingFlowStore((s) => s.goBackToZone);
  const toggleSeat = useBookingFlowStore((s) => s.toggleSeat);

  const { data: layout } = useSeatLayout(matchId ?? 0, sectionId ?? undefined);

  if (!layout) {
    return (
      <div className="py-20 text-center text-muted-foreground">
        좌석 정보를 불러오는 중...
      </div>
    );
  }

  return (
    <SeatGrid
      layout={layout}
      selectedIds={selectedSeats.map((s) => s.id)}
      onToggle={toggleSeat}
      onBack={goBackToZone}
    />
  );
}
