import { ArrowLeft } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { GradeAvailability, Seat } from "@/types";

const MAX_TICKETS_PER_USER = 2;

interface Props {
  seats: Seat[];
  grades: GradeAvailability[];
  selectedIds: number[];
  onToggle: (seat: Seat) => void;
  onBack: () => void;
}

export function SeatGrid({
  seats,
  grades,
  selectedIds,
  onToggle,
  onBack,
}: Props) {
  const seatsByRow = groupByRow(seats);
  const rows = Object.keys(seatsByRow).sort();

  const handleToggle = (seat: Seat) => {
    const isSelected = selectedIds.includes(seat.seatId);
    if (!isSelected && selectedIds.length >= MAX_TICKETS_PER_USER) {
      toast.error(`1인당 최대 ${MAX_TICKETS_PER_USER}매까지 예매 가능합니다.`);
      return;
    }
    onToggle(seat);
  };

  return (
    <div className="space-y-4 px-6 pt-3 pb-6">
      <div className="flex items-center justify-between">
        <Button variant="outline" size="sm" onClick={onBack} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          구역 선택으로 돌아가기
        </Button>
        <p className="text-xs text-muted-foreground">
          1인당 최대 {MAX_TICKETS_PER_USER}매까지 예매 가능합니다.
        </p>
      </div>

      {/* 스크린 */}
      <div className="flex justify-center">
        <div className="relative w-3/4 rounded-t-full border-4 border-gray-800 bg-gray-900 py-3 text-center text-white font-bold tracking-widest">
          MAIN SCREEN
        </div>
      </div>

      {/* 좌석 영역 */}
      <div className="rounded-xl border bg-gradient-to-b from-gray-50 to-white p-8 shadow-sm">
        <div className="space-y-3">
          {rows.map((row) => (
            <div key={row} className="flex items-center justify-center gap-4">
              <span className="w-8 text-center text-sm font-semibold text-gray-600 min-h-6">
                {row}
              </span>

              <div className="flex justify-center gap-1">
                {(seatsByRow[row] ?? []).map((seat, index) => {
                  const selected = selectedIds.includes(seat.seatId);
                  const sold = seat.status === "SOLD";
                  const reserved = seat.status === "RESERVED";
                  const disabled = sold || reserved;

                  const hasGapAfter =
                    (index + 1) % 8 === 0 &&
                    index + 1 !== (seatsByRow[row] ?? []).length;

                  return (
                    <div key={seat.seatId} className={hasGapAfter ? "mr-3" : ""}>
                      <button
                        type="button"
                        disabled={disabled}
                        onClick={() => handleToggle(seat)}
                        aria-label={seat.seatCode}
                        title={seat.seatCode}
                        style={{ backgroundColor: `#${seat.colorHex}` }}
                        className={cn(
                          "h-7 w-7 rounded transition-all duration-200",
                          "flex items-center justify-center text-xs font-medium",
                          disabled
                            ? "cursor-not-allowed opacity-40 grayscale"
                            : "cursor-pointer hover:scale-110 hover:shadow-md active:scale-95",
                          selected &&
                            "ring-2 ring-offset-1 ring-blue-500 shadow-lg scale-110 border-2 border-white",
                        )}
                      >
                        {selected && (
                          <span className="text-white drop-shadow-sm text-xs font-bold">
                            {seat.seatNo}
                          </span>
                        )}
                      </button>
                    </div>
                  );
                })}
              </div>

              <span className="w-8 text-center text-sm font-semibold text-gray-600 min-h-6">
                {row}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 범례 */}
      <div className="flex flex-wrap justify-center gap-5 px-6 py-4 bg-gray-50 rounded-lg text-sm">
        {grades.map((g) => (
          <div key={g.gradeCode} className="flex items-center gap-2">
            <div
              className="h-4 w-4 rounded"
              style={{ backgroundColor: `#${g.colorHex}` }}
            />
            <span>{g.gradeCode}석</span>
          </div>
        ))}
        <div className="flex items-center gap-2">
          <div className="h-4 w-4 rounded bg-gray-400 opacity-40 grayscale" />
          <span>판매 완료</span>
        </div>
      </div>

      {selectedIds.length > 0 && (
        <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <p className="text-sm text-gray-700">
            <span className="font-semibold text-blue-600">
              {selectedIds.length}개
            </span>
            의 좌석이 선택되었습니다.
          </p>
        </div>
      )}
    </div>
  );
}

function groupByRow(seats: Seat[]): Record<string, Seat[]> {
  return seats.reduce<Record<string, Seat[]>>((acc, s) => {
    (acc[s.rowLabel] ??= []).push(s);
    return acc;
  }, {});
}
