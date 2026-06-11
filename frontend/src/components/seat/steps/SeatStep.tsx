import { useBookingFlowStore } from "@/store";
import { useSeatSections, useSectionSeats } from "@/hooks";
import { Button } from "@/components/ui/button";
import { SeatGrid } from "../SeatGrid";

export function SeatStep() {
  const matchId = useBookingFlowStore((s) => s.matchId);
  const sectionId = useBookingFlowStore((s) => s.sectionId);
  const selectedSeats = useBookingFlowStore((s) => s.selectedSeats);
  const goBackToZone = useBookingFlowStore((s) => s.goBackToZone);
  const toggleSeat = useBookingFlowStore((s) => s.toggleSeat);

  const { data: sectionList } = useSeatSections(matchId);
  const grades = sectionList?.gradeAvailability ?? [];
  const { data: seatList, isLoading } = useSectionSeats(
    matchId ?? 0,
    sectionId ?? undefined,
  );

  // sectionId 가 없으면 구역 선택부터 다시
  if (!sectionId) {
    return (
      <div className="py-20 text-center space-y-3">
        <p className="text-sm text-muted-foreground">
          구역이 선택되지 않았습니다.
        </p>
        <Button variant="outline" onClick={goBackToZone}>
          구역 선택으로
        </Button>
      </div>
    );
  }

  if (isLoading || !seatList) {
    return (
      <div className="py-20 text-center text-muted-foreground">
        좌석 정보를 불러오는 중...
      </div>
    );
  }

  // 좌석 선택 시 현재 구역명을 함께 붙여서 store 에 저장 (사이드바/결제에서 표시용)
  const handleToggle = (seat: Parameters<typeof toggleSeat>[0]) => {
    toggleSeat({ ...seat, sectionName: seatList.sectionName });
  };

  return (
    <SeatGrid
      seats={seatList.seats}
      grades={grades}
      selectedIds={selectedSeats.map((s) => s.seatId)}
      onToggle={handleToggle}
      onBack={goBackToZone}
    />
  );
}
