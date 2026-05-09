import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SEAT_GRADE_BG_SOFT, SEAT_GRADE_COLORS } from "@/lib/constants";
import { cn } from "@/lib/utils";
import type { Seat, SeatLayout } from "@/types";

interface Props {
  layout: SeatLayout;
  selectedIds: number[];
  onToggle: (seat: Seat) => void;
  onBack: () => void;
}

export function SeatGrid({ layout, selectedIds, onToggle, onBack }: Props) {
  const seatsByRow = groupByRow(layout.seats);

  return (
    <div className="space-y-6 p-6">
      <Button
        variant="outline"
        size="sm"
        onClick={onBack}
        className="gap-2"
      >
        <ArrowLeft className="h-4 w-4" />
        구역 선택으로 돌아가기
      </Button>

      <div className="flex justify-center">
        <Button
          variant="secondary"
          className="rounded-full text-xs text-muted-foreground"
        >
          MAIN SCREEN
        </Button>
      </div>

      <div className="rounded-xl border bg-white p-6">
        <div className="space-y-2">
          {layout.rows.map((row) => (
            <div key={row} className="flex items-center gap-2">
              <span className="w-5 text-sm font-medium text-muted-foreground">
                {row}
              </span>
              <div className="flex flex-1 justify-center gap-1.5">
                {(seatsByRow[row] ?? []).map((seat) => {
                  const selected = selectedIds.includes(seat.id);
                  const sold = seat.status === "SOLD";
                  return (
                    <button
                      key={seat.id}
                      type="button"
                      disabled={sold}
                      onClick={() => onToggle(seat)}
                      aria-label={`${seat.row}열 ${seat.number}번`}
                      className={cn(
                        "h-6 w-6 rounded transition-transform",
                        sold
                          ? cn(
                              SEAT_GRADE_BG_SOFT[seat.gradeCode],
                              "cursor-not-allowed opacity-60",
                            )
                          : SEAT_GRADE_COLORS[seat.gradeCode],
                        selected && "ring-2 ring-offset-2 ring-[#FF6B47]",
                      )}
                    />
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function groupByRow(seats: Seat[]): Record<string, Seat[]> {
  return seats.reduce<Record<string, Seat[]>>((acc, s) => {
    (acc[s.row] ??= []).push(s);
    return acc;
  }, {});
}
