import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { formatPrice } from "@/lib/format";
import type { GradeAvailability, Seat } from "@/types";

interface Props {
  grades: GradeAvailability[];
  selected: Seat[];
  total: number;
  canSubmit: boolean;
  showGrades: boolean;
  onRemove: (seatId: number) => void;
  onSubmit: () => void;
}

export function SeatSidebar({
  grades,
  selected,
  total,
  canSubmit,
  showGrades,
  onRemove,
  onSubmit,
}: Props) {
  return (
    <aside className="flex h-full w-80 flex-col bg-white px-6 pt-16 pb-6">
      {showGrades && grades.length > 0 && (
        <>
          <div className="space-y-3">
            <h3 className="font-bold">등급별 잔여 좌석</h3>
            <ul className="space-y-2 text-sm">
              {grades.map((g) => (
                <li
                  key={g.gradeCode}
                  className="flex items-center justify-between"
                >
                  <div className="flex items-center gap-2">
                    <span
                      className="h-4 w-4 rounded-sm"
                      style={{ backgroundColor: `#${g.colorHex}` }}
                    />
                    <span>{g.gradeCode}석</span>
                  </div>
                  <span className="text-muted-foreground">
                    {g.availableCount}석
                  </span>
                </li>
              ))}
            </ul>
          </div>
          <Separator className="my-5" />
        </>
      )}

      <div className="flex-1 space-y-3 overflow-y-auto">
        <h3 className="font-bold">선택한 좌석</h3>
        {selected.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            선택한 좌석이 없습니다
          </p>
        ) : (
          <ul className="space-y-2">
            {selected.map((s) => (
              <li
                key={s.seatId}
                className="flex items-center justify-between rounded-lg bg-orange-50 px-3 py-2 text-sm"
              >
                <div>
                  <p className="font-medium">
                    {s.sectionName ? `${s.sectionName} ` : ""}
                    {s.seatCode}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatPrice(s.price)}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => onRemove(s.seatId)}
                  className="text-red-500 hover:text-red-700"
                  aria-label="좌석 제거"
                >
                  <X className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <Separator className="my-5" />
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="font-semibold">총 결제금액</span>
          <span className="text-xl font-bold text-[#054EFD]">
            {formatPrice(total)}
          </span>
        </div>
        <Button
          size="lg"
          disabled={!canSubmit}
          onClick={onSubmit}
          className="w-full bg-[#054EFD] hover:bg-[#3C76FE] disabled:bg-gray-200 disabled:text-gray-400"
        >
          좌석 선택 완료
        </Button>
      </div>
    </aside>
  );
}
