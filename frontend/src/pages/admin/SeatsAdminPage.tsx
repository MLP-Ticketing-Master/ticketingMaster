import { useMemo, useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { AdminCard } from "@/components/admin/AdminCard";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { SeatBulkFormDialog } from "@/components/admin/SeatBulkFormDialog";
import { SeatFormDialog } from "@/components/admin/SeatFormDialog";
import { SeatGradeFormDialog } from "@/components/admin/SeatGradeFormDialog";
import { SectionFormDialog } from "@/components/admin/SectionFormDialog";
import { SeatStatusGrid } from "@/components/admin/SeatStatusGrid";
import {
  useAdminEvents,
  useAdminMatches,
  useAdminSeatGrades,
  useAdminSections,
  useDeleteSeatGradeMutation,
  useDeleteSectionMutation,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { formatPrice, normalizeColorHex } from "@/lib/format";
import type {
  AdminSeatGradeResponse,
  AdminSeatResponse,
  AdminSectionResponse,
} from "@/types";

export default function SeatsAdminPage() {
  const eventsQuery = useAdminEvents();
  const events = eventsQuery.data?.content ?? [];

  const [eventIdStr, setEventIdStr] = useState<string>("");
  const eventId = eventIdStr ? Number(eventIdStr) : null;

  if (!eventIdStr && events.length > 0) {
    setEventIdStr(String(events[0].eventId));
  }

  const gradesQuery = useAdminSeatGrades(eventId);
  const sectionsQuery = useAdminSections(eventId);
  const grades = gradesQuery.data ?? [];
  const sections = sectionsQuery.data ?? [];

  const matchesQuery = useAdminMatches(eventId ?? undefined);
  const matches = matchesQuery.data?.content ?? [];

  const [matchIdStr, setMatchIdStr] = useState<string>("");
  const matchId = matchIdStr ? Number(matchIdStr) : null;

  const handleEventChange = (v: string) => {
    setEventIdStr(v);
    setMatchIdStr("");
  };

  if (!matchIdStr && matches.length > 0 && eventId !== null) {
    setMatchIdStr(String(matches[0].id));
  }

  // 등급 / 구역 / 좌석 모달
  const [editingGrade, setEditingGrade] =
    useState<AdminSeatGradeResponse | null>(null);
  const [gradeDialogOpen, setGradeDialogOpen] = useState(false);

  const [editingSection, setEditingSection] =
    useState<AdminSectionResponse | null>(null);
  const [sectionDialogOpen, setSectionDialogOpen] = useState(false);

  const [seatBulkDialogOpen, setSeatBulkDialogOpen] = useState(false);

  const [editingSeat, setEditingSeat] = useState<AdminSeatResponse | null>(
    null,
  );
  const [seatDialogOpen, setSeatDialogOpen] = useState(false);

  // 삭제 mutations
  const deleteGradeMutation = useDeleteSeatGradeMutation(eventId ?? 0);
  const deleteSectionMutation = useDeleteSectionMutation(eventId ?? 0);

  const handleSeatClick = (seat: AdminSeatResponse) => {
    setEditingSeat(seat);
    setSeatDialogOpen(true);
  };

  const selectedEvent = useMemo(
    () => events.find((e) => e.eventId === eventId) ?? null,
    [events, eventId],
  );
  const selectedMatch = useMemo(
    () => matches.find((m) => m.id === matchId) ?? null,
    [matches, matchId],
  );

  const openGradeCreate = () => {
    setEditingGrade(null);
    setGradeDialogOpen(true);
  };
  const openGradeEdit = (g: AdminSeatGradeResponse) => {
    setEditingGrade(g);
    setGradeDialogOpen(true);
  };
  const handleGradeDelete = (g: AdminSeatGradeResponse) => {
    if (!confirm(`'${g.gradeCode}석' 등급을 삭제하시겠습니까?`)) return;
    deleteGradeMutation.mutate(g.seatGradeId, {
      onSuccess: () => toast.success("등급이 삭제되었습니다."),
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "등급 삭제에 실패했습니다.")),
    });
  };

  const openSectionCreate = () => {
    setEditingSection(null);
    setSectionDialogOpen(true);
  };
  const openSectionEdit = (s: AdminSectionResponse) => {
    setEditingSection(s);
    setSectionDialogOpen(true);
  };
  const handleSectionDelete = (s: AdminSectionResponse) => {
    if (!confirm(`'${s.name}'을(를) 삭제하시겠습니까?`)) return;
    deleteSectionMutation.mutate(s.sectionId, {
      onSuccess: () => toast.success("구역이 삭제되었습니다."),
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "구역 삭제에 실패했습니다.")),
    });
  };

  const nextSectionOrder =
    sections.length > 0
      ? Math.max(...sections.map((s) => s.displayOrder)) + 1
      : 1;

  return (
    <div className="space-y-6">
      {/* ── 좌석 등급 관리 ── */}
      <AdminCard
        title="좌석 등급 관리"
        headerClassName="mb-3"
        action={
          <Button
            onClick={openGradeCreate}
            disabled={!eventId}
            className="bg-[#054EFD] hover:bg-[#3C76FE]"
          >
            <Plus className="mr-1 h-4 w-4" />
            등급 등록
          </Button>
        }
      >
        <div className="space-y-4">
          <div className="w-80">
            <Select value={eventIdStr} onValueChange={handleEventChange}>
              <SelectTrigger>
                <SelectValue placeholder="대회 선택" />
              </SelectTrigger>
              <SelectContent>
                {events.map((e) => (
                  <SelectItem key={e.eventId} value={String(e.eventId)}>
                    {e.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {gradesQuery.isLoading && (
            <p className="py-8 text-center text-sm text-muted-foreground">
              불러오는 중...
            </p>
          )}

          {!gradesQuery.isLoading && grades.length === 0 && (
            <p className="py-8 text-center text-sm text-muted-foreground">
              등록된 좌석 등급이 없습니다.
            </p>
          )}

          <div className="space-y-2">
            {grades.map((g) => (
              <div
                key={g.seatGradeId}
                className="flex items-center justify-between rounded-xl border bg-white px-5 py-3.5 shadow-sm"
              >
                <div className="flex items-center gap-4">
                  <span
                    className="h-10 w-10 rounded-md"
                    style={{ backgroundColor: normalizeColorHex(g.colorHex) }}
                  />
                  <p className="text-base font-bold">{g.gradeCode}석</p>
                </div>
                <div className="flex items-center gap-3">
                  <p className="text-lg font-bold tabular-nums">
                    {formatPrice(g.price)}
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openGradeEdit(g)}
                  >
                    수정
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleGradeDelete(g)}
                    disabled={deleteGradeMutation.isPending}
                    className="h-8 w-8 text-red-500 hover:bg-red-50 hover:text-red-600"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </AdminCard>

      {/* ── 구역 관리 ── */}
      <AdminCard
        title="구역 관리"
        headerClassName="mb-3"
        action={
          <Button
            onClick={openSectionCreate}
            disabled={!eventId}
            className="bg-[#054EFD] hover:bg-[#3C76FE]"
          >
            <Plus className="mr-1 h-4 w-4" />
            구역 등록
          </Button>
        }
      >
        {sectionsQuery.isLoading ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            불러오는 중...
          </p>
        ) : sections.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            등록된 구역이 없습니다.
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            {[...sections]
              .sort((a, b) => a.displayOrder - b.displayOrder)
              .map((s, idx) => (
                <div
                  key={s.sectionId}
                  className="flex flex-col gap-3 rounded-xl border bg-white p-4 shadow-sm"
                >
                  <div className="flex items-center gap-2">
                    <span
                      className={`h-4 w-4 rounded-sm ${SECTION_COLORS[idx] ?? "bg-gray-400"}`}
                    />
                    <p className="text-base font-bold">{s.name}</p>
                  </div>
                  <p className="min-h-[1.25rem] text-sm text-muted-foreground">
                    {s.description ?? "—"}
                  </p>
                  <div className="mt-auto flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex-1"
                      onClick={() => openSectionEdit(s)}
                    >
                      수정
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleSectionDelete(s)}
                      disabled={deleteSectionMutation.isPending}
                      className="flex-1 border-red-200 text-red-500 hover:bg-red-50 hover:text-red-600"
                    >
                      삭제
                    </Button>
                  </div>
                </div>
              ))}
          </div>
        )}
      </AdminCard>

      {/* ── 회차별 좌석 현황 ── */}
      <Card className="!gap-0 p-6">
        <div className="mb-3 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h2 className="text-xl font-bold">회차별 좌석 현황</h2>
            <div className="w-72">
              <Select
                value={matchIdStr}
                onValueChange={setMatchIdStr}
                disabled={matches.length === 0}
              >
                <SelectTrigger>
                  <SelectValue placeholder="회차 선택" />
                </SelectTrigger>
                <SelectContent>
                  {matches.map((m) => (
                    <SelectItem key={m.id} value={String(m.id)}>
                      {formatMatchOption(m.startAt, m.roundLabel)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <Button
            onClick={() => setSeatBulkDialogOpen(true)}
            disabled={!matchId || sections.length === 0 || grades.length === 0}
            className="bg-[#054EFD] hover:bg-[#3C76FE]"
          >
            <Plus className="mr-1 h-4 w-4" />
            좌석 일괄 등록
          </Button>
        </div>

        {selectedEvent && (
          <p className="mb-5 text-sm text-muted-foreground">
            대회: {selectedEvent.title}
          </p>
        )}

        <SeatStatusGrid
          matchId={matchId}
          grades={grades}
          sections={sections}
          disabled={!selectedMatch}
          onSeatClick={handleSeatClick}
        />
        <p className="mt-3 text-xs text-muted-foreground">
          ※ AVAILABLE 상태인 좌석을 클릭하면 구역/등급 변경 또는 삭제할 수 있습니다 (예매된 좌석은 수정 불가).
        </p>
      </Card>

      <SeatGradeFormDialog
        open={gradeDialogOpen}
        onOpenChange={setGradeDialogOpen}
        eventId={eventId ?? 0}
        grade={editingGrade}
      />
      <SectionFormDialog
        open={sectionDialogOpen}
        onOpenChange={setSectionDialogOpen}
        eventId={eventId ?? 0}
        section={editingSection}
        defaultDisplayOrder={nextSectionOrder}
      />
      {matchId !== null && (
        <>
          <SeatBulkFormDialog
            open={seatBulkDialogOpen}
            onOpenChange={setSeatBulkDialogOpen}
            matchId={matchId}
            sections={sections}
            grades={grades}
          />
          <SeatFormDialog
            open={seatDialogOpen}
            onOpenChange={setSeatDialogOpen}
            matchId={matchId}
            seat={editingSeat}
            sections={sections}
            grades={grades}
          />
        </>
      )}
    </div>
  );
}

// ZoneSelector 와 동일한 displayOrder 인덱스 매핑
const SECTION_COLORS = [
  "bg-blue-500",
  "bg-emerald-500",
  "bg-purple-600",
  "bg-red-500",
];

function formatMatchOption(startAt: string, roundLabel: string): string {
  const d = new Date(startAt);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mi = String(d.getMinutes()).padStart(2, "0");
  return `${yyyy}.${mm}.${dd} ${hh}:${mi} (${roundLabel})`;
}
