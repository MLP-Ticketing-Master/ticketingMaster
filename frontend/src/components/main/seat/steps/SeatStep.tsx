import { useBookingFlowStore } from "@/store";
import { useSeatSections, useSectionSeats } from "@/hooks";
import { SeatGrid } from "../SeatGrid";

export function SeatStep() {
  const matchId = useBookingFlowStore((s) => s.matchId);
  const sectionId = useBookingFlowStore((s) => s.sectionId);
  const selectedSeats = useBookingFlowStore((s) => s.selectedSeats);
  const goBackToZone = useBookingFlowStore((s) => s.goBackToZone);
  const toggleSeat = useBookingFlowStore((s) => s.toggleSeat);

  const { data: seatList } = useSectionSeats(
    matchId ?? 0,
    sectionId ?? undefined,
  );
  const { data: sectionList } = useSeatSections(matchId);
  const grades = sectionList?.gradeAvailability ?? [];

  if (!seatList) {
    return (
      <div className="py-20 text-center text-muted-foreground">
        좌석 정보를 불러오는 중...
      </div>
    );
  }

  return (
    <SeatGrid
      seats={seatList.seats}
      grades={grades}
      selectedIds={selectedSeats.map((s) => s.seatId)}
      onToggle={toggleSeat}
      onBack={goBackToZone}
    />
  );
}
