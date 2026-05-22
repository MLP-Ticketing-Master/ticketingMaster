import { useMemo } from "react";
import { useBookingFlowStore } from "@/store";
import { useEventDetail, useSeatLayout } from "@/hooks";
import { SeatGrid } from "../SeatGrid";

export function SeatStep() {
  const eventId = useBookingFlowStore((s) => s.eventId);
  const matchId = useBookingFlowStore((s) => s.matchId);
  const sectionId = useBookingFlowStore((s) => s.sectionId);
  const selectedSeats = useBookingFlowStore((s) => s.selectedSeats);
  const goBackToZone = useBookingFlowStore((s) => s.goBackToZone);
  const toggleSeat = useBookingFlowStore((s) => s.toggleSeat);

  const { data: event } = useEventDetail(eventId ?? 0);
  const { data: layout } = useSeatLayout(matchId ?? 0, sectionId ?? undefined);

  // event.seatGrades → SeatGrade[] 어댑터
  const grades = useMemo(() => {
    if (!event?.seatGrades) return [];
    return event.seatGrades.map((sg) => ({
      code: sg.gradeCode,
      name: `${sg.gradeCode}석`,
      price: sg.price,
      color: sg.colorHex,
      sortOrder: 0,
      remaining: 0,
    }));
  }, [event?.seatGrades]);

  if (!layout) {
    return (
      <div className="py-20 text-center text-muted-foreground">
        좌석 정보를 불러오는 중...
      </div>
    );
  }

  return (
    <SeatGrid
      grades={grades}
      layout={layout}
      selectedIds={selectedSeats.map((s) => s.id)}
      onToggle={toggleSeat}
      onBack={goBackToZone}
    />
  );
}
