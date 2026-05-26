import { useMemo } from "react";
import { useAdminSeats } from "@/hooks";
import { formatPrice, normalizeColorHex } from "@/lib/format";
import type {
  AdminSeatGradeResponse,
  AdminSeatResponse,
  AdminSectionResponse,
} from "@/types";

interface Props {
  matchId: number | null;
  grades: AdminSeatGradeResponse[];
  sections: AdminSectionResponse[];
  disabled?: boolean;
  /** AVAILABLE 좌석 클릭 시 호출 — 단건 수정/삭제 모달 오픈 */
  onSeatClick?: (seat: AdminSeatResponse) => void;
}

// ZoneSelector / SeatsAdminPage 와 동일한 displayOrder 인덱스 매핑
const SECTION_BORDER_COLORS = [
  "border-blue-300",
  "border-emerald-300",
  "border-purple-300",
  "border-red-300",
];
const SECTION_HEADER_BG = [
  "bg-blue-100",
  "bg-emerald-100",
  "bg-purple-100",
  "bg-red-100",
];
const SECTION_HEADER_TEXT = [
  "text-blue-700",
  "text-emerald-700",
  "text-purple-700",
  "text-red-700",
];

// seatCode 형식: "{gradeCode}-{rowLabel}-{seatNo}"
function parseSeatCode(code: string): { rowLabel: string; seatNo: number } {
  const parts = code.split("-");
  if (parts.length < 2) return { rowLabel: "?", seatNo: 0 };
  const seatNo = Number(parts[parts.length - 1]) || 0;
  const rowLabel = parts[parts.length - 2];
  return { rowLabel, seatNo };
}

export function SeatStatusGrid({
  matchId,
  grades,
  sections,
  disabled,
  onSeatClick,
}: Props) {
  const { data: seats = [], isLoading, isError } = useAdminSeats(matchId);

  const colorByGrade = useMemo(() => {
    const map = new Map<string, string>();
    grades.forEach((g) => map.set(g.gradeCode, normalizeColorHex(g.colorHex)));
    return map;
  }, [grades]);

  const sectionsInOrder = useMemo(
    () => [...sections].sort((a, b) => a.displayOrder - b.displayOrder),
    [sections],
  );

  // 모든 rowLabel 수집 + 정렬 (구역마다 다른 행을 가질 수 있어서 전체 union)
  const allRowLabels = useMemo(() => {
    const labels = new Set<string>();
    seats.forEach((s) => {
      const { rowLabel } = parseSeatCode(s.seatCode);
      labels.add(rowLabel);
    });
    return Array.from(labels).sort((a, b) => a.localeCompare(b));
  }, [seats]);

  // 구역별 → rowLabel → 좌석 목록 (seatNo 정렬)
  const seatsBySection = useMemo(() => {
    const map = new Map<string, Map<string, AdminSeatResponse[]>>();
    sectionsInOrder.forEach((s) => map.set(s.name, new Map()));
    seats.forEach((seat) => {
      const { rowLabel } = parseSeatCode(seat.seatCode);
      const byRow = map.get(seat.sectionName);
      if (!byRow) return;
      if (!byRow.has(rowLabel)) byRow.set(rowLabel, []);
      byRow.get(rowLabel)!.push(seat);
    });
    map.forEach((byRow) =>
      byRow.forEach((arr) =>
        arr.sort(
          (a, b) =>
            parseSeatCode(a.seatCode).seatNo - parseSeatCode(b.seatCode).seatNo,
        ),
      ),
    );
    return map;
  }, [seats, sectionsInOrder]);

  // 통계
  const stats = useMemo(() => {
    const total = seats.length;
    const available = seats.filter((s) => s.status === "AVAILABLE").length;
    const sold = seats.filter(
      (s) => s.status === "SOLD" || s.status === "RESERVED",
    ).length;
    return { total, available, sold };
  }, [seats]);

  if (disabled) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        회차를 선택해주세요.
      </p>
    );
  }

  if (isLoading) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        좌석 정보를 불러오는 중...
      </p>
    );
  }

  if (isError) {
    return (
      <p className="py-12 text-center text-sm text-red-500">
        좌석 정보를 불러오는 데 실패했습니다.
      </p>
    );
  }

  if (seats.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        등록된 좌석이 없습니다.
      </p>
    );
  }

  const renderSeat = (seat: AdminSeatResponse) => {
    const color = colorByGrade.get(seat.gradeCode) ?? "#9ca3af";
    const isAvailable = seat.status === "AVAILABLE";
    const clickable = isAvailable && !!onSeatClick;
    return (
      <span
        key={seat.seatId}
        title={
          clickable
            ? `${seat.seatCode} · 클릭하여 수정/삭제`
            : `${seat.seatCode} · ${seat.status}`
        }
        onClick={clickable ? () => onSeatClick(seat) : undefined}
        className={`h-4 w-4 shrink-0 rounded-sm ${
          clickable
            ? "cursor-pointer transition-transform hover:scale-125"
            : ""
        }`}
        style={{
          backgroundColor: color,
          opacity: isAvailable ? 1 : 0.3,
        }}
      />
    );
  };

  return (
    <div className="space-y-5">
      {/* 좌석 등급 */}
      <div className="space-y-2">
        <p className="text-sm font-semibold">좌석 등급</p>
        <div className="flex flex-wrap gap-4">
          {grades.map((g) => (
            <div key={g.seatGradeId} className="flex items-center gap-2">
              <span
                className="h-4 w-4 rounded-sm"
                style={{ backgroundColor: normalizeColorHex(g.colorHex) }}
              />
              <span className="text-sm">
                {g.gradeCode}석 ({formatPrice(g.price)})
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 그리드 — 구역별 컬럼으로 분리, 가로 스크롤 가능 */}
      <div className="overflow-x-auto rounded-lg border bg-gray-50 px-6 py-8">
        <div className="mb-6 flex justify-center">
          <div className="rounded-full bg-gray-300 px-8 py-2 text-sm font-semibold text-gray-700">
            MAIN SCREEN
          </div>
        </div>

        <div className="flex items-start gap-3">
          {/* 행 라벨 컬럼 — section header(~28px) 만큼 아래로 밀어서 좌석과 정렬 */}
          <div className="flex flex-col gap-1.5 pt-[44px]">
            {allRowLabels.map((label) => (
              <span
                key={label}
                className="flex h-4 w-5 items-center justify-center text-xs font-semibold text-muted-foreground"
              >
                {label}
              </span>
            ))}
          </div>

          {/* 구역별 컬럼 */}
          {sectionsInOrder.map((section, idx) => {
            const borderClass =
              SECTION_BORDER_COLORS[idx] ?? "border-gray-300";
            const headerBg = SECTION_HEADER_BG[idx] ?? "bg-gray-100";
            const headerText = SECTION_HEADER_TEXT[idx] ?? "text-gray-700";
            const byRow = seatsBySection.get(section.name);
            return (
              <div
                key={section.sectionId}
                className={`flex flex-col gap-1.5 rounded-lg border-2 ${borderClass} bg-white p-2`}
              >
                <div
                  className={`mb-1 rounded px-2 py-1 text-center text-xs font-semibold ${headerBg} ${headerText}`}
                >
                  {section.name}
                </div>
                {allRowLabels.map((label) => {
                  const rowSeats = byRow?.get(label) ?? [];
                  return (
                    <div key={label} className="flex h-4 gap-0.5">
                      {rowSeats.map(renderSeat)}
                    </div>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>

      {/* 통계 */}
      <div className="grid grid-cols-3 gap-3">
        <div className="space-y-1 rounded-xl border bg-rose-50 p-4">
          <p className="text-xs text-muted-foreground">전체 좌석</p>
          <p className="text-2xl font-bold tabular-nums">{stats.total}</p>
        </div>
        <div className="space-y-1 rounded-xl border bg-emerald-50 p-4">
          <p className="text-xs text-muted-foreground">판매 가능</p>
          <p className="text-2xl font-bold tabular-nums text-emerald-600">
            {stats.available}
          </p>
        </div>
        <div className="space-y-1 rounded-xl border bg-red-50 p-4">
          <p className="text-xs text-muted-foreground">판매 완료</p>
          <p className="text-2xl font-bold tabular-nums text-red-600">
            {stats.sold}
          </p>
        </div>
      </div>
    </div>
  );
}
