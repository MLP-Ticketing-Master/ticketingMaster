import { useMemo } from "react";
import { X } from "lucide-react";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { useBookingFlowStore } from "@/store";
import {
  useEventDetail,
  useMatches,
  useSeatGrades,
  useSeatLayout,
  useSections,
  useCreateBookingMutation,
} from "@/hooks";
import { formatShortDate, formatTime } from "@/lib/format";
import { SeatSidebar } from "./SeatSidebar";
import { ZoneSelector } from "./ZoneSelector";
import { SeatGrid } from "./SeatGrid";
import { toast } from "sonner";

export function SeatSelectionDialog() {
  const open = useBookingFlowStore((s) => s.open);
  const step = useBookingFlowStore((s) => s.step);
  const eventId = useBookingFlowStore((s) => s.eventId);
  const matchId = useBookingFlowStore((s) => s.matchId);
  const sectionId = useBookingFlowStore((s) => s.sectionId);
  const selectedSeats = useBookingFlowStore((s) => s.selectedSeats);
  const closeFlow = useBookingFlowStore((s) => s.closeFlow);
  const goToSeat = useBookingFlowStore((s) => s.goToSeat);
  const goBackToZone = useBookingFlowStore((s) => s.goBackToZone);
  const toggleSeat = useBookingFlowStore((s) => s.toggleSeat);
  const removeSeat = useBookingFlowStore((s) => s.removeSeat);

  const { data: event } = useEventDetail(eventId ?? 0);
  const { data: matches = [] } = useMatches(eventId ?? undefined);
  const { data: sections = [] } = useSections(eventId ?? 0);
  const { data: grades = [] } = useSeatGrades(eventId ?? 0);
  const { data: layout } = useSeatLayout(matchId ?? 0, sectionId ?? undefined);

  const createBooking = useCreateBookingMutation();

  const match = matches.find((m) => m.id === matchId);

  const total = useMemo(() => {
    if (selectedSeats.length === 0) return 0;
    const priceMap = new Map(grades.map((g) => [g.code, g.price]));
    return selectedSeats.reduce(
      (sum, s) => sum + (priceMap.get(s.gradeCode) ?? 0),
      0,
    );
  }, [selectedSeats, grades]);

  const handleSubmit = () => {
    if (!matchId) return;
    createBooking.mutate(
      { matchId, seatIds: selectedSeats.map((s) => s.id) },
      {
        onSuccess: () => {
          toast.success("예매가 완료되었습니다.");
          closeFlow();
        },
      },
    );
  };

  if (!event || !match) return null;

  return (
    <Dialog open={open} onOpenChange={(v) => !v && closeFlow()}>
      <DialogContent
        showCloseButton={false}
        className="!max-w-6xl gap-0 overflow-hidden p-0"
      >
        <header className="flex items-start justify-between bg-[#2D2F3E] px-8 py-6 text-white">
          <div>
            <h2 className="text-xl font-bold">{event.title}</h2>
            <p className="mt-1 text-sm text-gray-300">
              {formatShortDate(match.startAt)} {formatTime(match.startAt)}
            </p>
          </div>
          <button
            type="button"
            onClick={closeFlow}
            className="text-gray-300 hover:text-white"
            aria-label="닫기"
          >
            <X className="h-6 w-6" />
          </button>
        </header>

        <div className="grid grid-cols-[1fr_320px] bg-gray-50">
          <div className="max-h-[70vh] overflow-y-auto">
            {step === "ZONE" ? (
              <ZoneSelector sections={sections} onSelect={goToSeat} />
            ) : layout ? (
              <SeatGrid
                layout={layout}
                selectedIds={selectedSeats.map((s) => s.id)}
                onToggle={toggleSeat}
                onBack={goBackToZone}
              />
            ) : (
              <div className="py-20 text-center text-muted-foreground">
                좌석 정보를 불러오는 중...
              </div>
            )}
          </div>

          <SeatSidebar
            grades={grades}
            selected={selectedSeats}
            total={total}
            canSubmit={selectedSeats.length > 0}
            onRemove={removeSeat}
            onSubmit={handleSubmit}
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}
