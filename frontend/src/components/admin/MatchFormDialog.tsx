import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useAdminEvents,
  useAdminMatchDetail,
  useCreateMatchMutation,
  useTeams,
  useUpdateMatchMutation,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import type {
  AdminMatchCreateRequest,
  AdminMatchStatus,
  AdminMatchUpdateRequest,
} from "@/types";

const STATUS_OPTIONS: { value: AdminMatchStatus; label: string }[] = [
  { value: "SCHEDULED", label: "예정" },
  { value: "LIVE", label: "진행중" },
  { value: "FINISHED", label: "종료" },
  { value: "CANCELED", label: "취소" },
];

const NONE = "NONE";

interface FormState {
  eventId: string;          // Select 컴포넌트는 string 만 다룸
  roundLabel: string;
  homeTeamId: string;       // "" or "NONE" = 미지정
  awayTeamId: string;
  matchDate: string;        // "YYYY-MM-DD"
  startAt: string;          // datetime-local: "YYYY-MM-DDTHH:mm"
  endAt: string;
  bookingOpenAt: string;
  bookingCloseAt: string;
  status: AdminMatchStatus;
}

const EMPTY_FORM: FormState = {
  eventId: "",
  roundLabel: "",
  homeTeamId: NONE,
  awayTeamId: NONE,
  matchDate: "",
  startAt: "",
  endAt: "",
  bookingOpenAt: "",
  bookingCloseAt: "",
  status: "SCHEDULED",
};

// 백엔드 LocalDateTime ("2026-04-24T18:30:00") → input value ("2026-04-24T18:30")
const toLocalInput = (iso: string | null): string => {
  if (!iso) return "";
  return iso.slice(0, 16);
};

// input value → 백엔드 형식 (초 0 부착)
const toBackendDateTime = (s: string): string => {
  if (!s) return "";
  return s.length === 16 ? `${s}:00` : s;
};

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  matchId: number | null;   // null이면 신규 등록
}

export function MatchFormDialog({ open, onOpenChange, matchId }: Props) {
  const isEdit = matchId !== null;
  const [form, setForm] = useState<FormState>(EMPTY_FORM);

  const eventsQuery = useAdminEvents();
  const events = eventsQuery.data?.content ?? [];

  const teamsQuery = useTeams("ALL");
  const teams = teamsQuery.data ?? [];

  const detailQuery = useAdminMatchDetail(open && isEdit ? matchId : null);

  const createMutation = useCreateMatchMutation();
  const updateMutation = useUpdateMatchMutation();
  const isPending = createMutation.isPending || updateMutation.isPending;

  // 선택된 대회의 sportType 으로 팀 후보 필터
  const selectedEvent = useMemo(() => {
    const id = Number(form.eventId);
    if (!id) return null;
    return events.find((e) => e.eventId === id) ?? null;
  }, [events, form.eventId]);

  const teamCandidates = useMemo(() => {
    if (!selectedEvent) return teams;
    return teams.filter((t) => t.sportType === selectedEvent.sportType);
  }, [teams, selectedEvent]);

  // 모달 상태/상세 데이터에 따라 폼 초기화
  useEffect(() => {
    if (!open) return;

    if (!isEdit) {
      setForm(EMPTY_FORM);
      return;
    }

    if (detailQuery.data) {
      const d = detailQuery.data;
      setForm({
        eventId: String(d.eventId),
        roundLabel: d.roundLabel,
        homeTeamId: d.homeTeamId !== null ? String(d.homeTeamId) : NONE,
        awayTeamId: d.awayTeamId !== null ? String(d.awayTeamId) : NONE,
        matchDate: d.matchDate,
        startAt: toLocalInput(d.startAt),
        endAt: toLocalInput(d.endAt),
        bookingOpenAt: toLocalInput(d.bookingOpenAt),
        bookingCloseAt: toLocalInput(d.bookingCloseAt),
        status: d.status,
      });
    }
  }, [open, isEdit, detailQuery.data]);

  const update = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const parseTeamId = (v: string): number | undefined =>
    v === NONE || v === "" ? undefined : Number(v);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!isEdit && !form.eventId) return toast.error("대회를 선택해주세요.");
    if (!form.roundLabel.trim()) return toast.error("회차 라벨을 입력해주세요.");
    if (!form.matchDate) return toast.error("경기 날짜를 입력해주세요.");
    if (!form.startAt) return toast.error("시작 시간을 입력해주세요.");
    if (!form.bookingOpenAt)
      return toast.error("예매 시작 시간을 입력해주세요.");
    if (!form.bookingCloseAt)
      return toast.error("예매 종료 시간을 입력해주세요.");
    if (form.bookingOpenAt >= form.bookingCloseAt)
      return toast.error("예매 시작은 예매 종료 이전이어야 합니다.");
    if (form.bookingCloseAt > form.startAt)
      return toast.error("예매 종료는 경기 시작 이전이어야 합니다.");
    if (form.endAt && form.endAt < form.startAt)
      return toast.error("종료 시간은 시작 시간 이후여야 합니다.");

    if (isEdit && matchId !== null) {
      const body: AdminMatchUpdateRequest = {
        roundLabel: form.roundLabel.trim(),
        homeTeamId: parseTeamId(form.homeTeamId),
        awayTeamId: parseTeamId(form.awayTeamId),
        matchDate: form.matchDate,
        startAt: toBackendDateTime(form.startAt),
        endAt: form.endAt ? toBackendDateTime(form.endAt) : undefined,
        bookingOpenAt: toBackendDateTime(form.bookingOpenAt),
        bookingCloseAt: toBackendDateTime(form.bookingCloseAt),
        status: form.status,
      };
      updateMutation.mutate(
        { matchId, body },
        {
          onSuccess: () => {
            toast.success("회차가 수정되었습니다.");
            onOpenChange(false);
          },
          onError: (err) =>
            toast.error(resolveErrorMessage(err, "회차 수정에 실패했습니다.")),
        },
      );
    } else {
      const body: AdminMatchCreateRequest = {
        roundLabel: form.roundLabel.trim(),
        matchDate: form.matchDate,
        startAt: toBackendDateTime(form.startAt),
        endAt: form.endAt ? toBackendDateTime(form.endAt) : undefined,
        bookingOpenAt: toBackendDateTime(form.bookingOpenAt),
        bookingCloseAt: toBackendDateTime(form.bookingCloseAt),
        homeTeamId: parseTeamId(form.homeTeamId),
        awayTeamId: parseTeamId(form.awayTeamId),
      };
      createMutation.mutate(
        { eventId: Number(form.eventId), body },
        {
          onSuccess: () => {
            toast.success("회차가 등록되었습니다.");
            onOpenChange(false);
          },
          onError: (err) =>
            toast.error(resolveErrorMessage(err, "회차 등록에 실패했습니다.")),
        },
      );
    }
  };

  const loadingDetail = isEdit && detailQuery.isLoading;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "회차 수정" : "회차 등록"}</DialogTitle>
        </DialogHeader>

        {loadingDetail ? (
          <p className="py-12 text-center text-sm text-muted-foreground">
            불러오는 중...
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* ── 대회 / 회차 ── */}
            <Section title="기본 정보">
              <Field label="대회 *" htmlFor="m-event">
                <Select
                  value={form.eventId}
                  onValueChange={(v) => {
                    update("eventId", v);
                    // 대회 바뀌면 팀 sportType 매칭이 깨질 수 있어 팀 초기화
                    update("homeTeamId", NONE);
                    update("awayTeamId", NONE);
                  }}
                  disabled={isEdit}
                >
                  <SelectTrigger id="m-event">
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
              </Field>
              <Field label="회차 라벨 *" htmlFor="m-round">
                <Input
                  id="m-round"
                  value={form.roundLabel}
                  onChange={(e) => update("roundLabel", e.target.value)}
                  placeholder="예: 결승 1경기"
                  maxLength={50}
                  required
                />
              </Field>
            </Section>

            {/* ── 대진 ── */}
            <Section title="대진 (선택)">
              <div className="grid grid-cols-2 gap-3">
                <Field label="홈팀" htmlFor="m-home">
                  <Select
                    value={form.homeTeamId}
                    onValueChange={(v) => update("homeTeamId", v)}
                  >
                    <SelectTrigger id="m-home">
                      <SelectValue placeholder="미정" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE}>미정</SelectItem>
                      {teamCandidates.map((t) => (
                        <SelectItem key={t.teamId} value={String(t.teamId)}>
                          {t.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field label="어웨이팀" htmlFor="m-away">
                  <Select
                    value={form.awayTeamId}
                    onValueChange={(v) => update("awayTeamId", v)}
                  >
                    <SelectTrigger id="m-away">
                      <SelectValue placeholder="미정" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE}>미정</SelectItem>
                      {teamCandidates.map((t) => (
                        <SelectItem key={t.teamId} value={String(t.teamId)}>
                          {t.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              </div>
            </Section>

            {/* ── 일정 ── */}
            <Section title="일정">
              <Field label="경기 날짜 *" htmlFor="m-date">
                <Input
                  id="m-date"
                  type="date"
                  value={form.matchDate}
                  onChange={(e) => update("matchDate", e.target.value)}
                  required
                />
              </Field>
              <div className="grid grid-cols-2 gap-3">
                <Field label="시작 시간 *" htmlFor="m-start">
                  <Input
                    id="m-start"
                    type="datetime-local"
                    value={form.startAt}
                    onChange={(e) => update("startAt", e.target.value)}
                    required
                  />
                </Field>
                <Field label="종료 시간" htmlFor="m-end">
                  <Input
                    id="m-end"
                    type="datetime-local"
                    value={form.endAt}
                    onChange={(e) => update("endAt", e.target.value)}
                  />
                </Field>
              </div>
            </Section>

            {/* ── 예매 정책 ── */}
            <Section title="예매 정책">
              <div className="grid grid-cols-2 gap-3">
                <Field label="예매 시작 *" htmlFor="m-bopen">
                  <Input
                    id="m-bopen"
                    type="datetime-local"
                    value={form.bookingOpenAt}
                    onChange={(e) => update("bookingOpenAt", e.target.value)}
                    required
                  />
                </Field>
                <Field label="예매 종료 *" htmlFor="m-bclose">
                  <Input
                    id="m-bclose"
                    type="datetime-local"
                    value={form.bookingCloseAt}
                    onChange={(e) => update("bookingCloseAt", e.target.value)}
                    required
                  />
                </Field>
              </div>
              {isEdit && (
                <Field label="상태" htmlFor="m-status">
                  <Select
                    value={form.status}
                    onValueChange={(v) =>
                      update("status", v as AdminMatchStatus)
                    }
                  >
                    <SelectTrigger id="m-status">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {STATUS_OPTIONS.map((opt) => (
                        <SelectItem key={opt.value} value={opt.value}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              )}
            </Section>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isPending}
              >
                취소
              </Button>
              <Button
                type="submit"
                disabled={isPending}
                className="bg-[#054EFD] hover:bg-[#3C76FE]"
              >
                {isPending ? "처리 중..." : isEdit ? "수정" : "등록"}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-3">
      <h4 className="text-sm font-semibold text-gray-700">{title}</h4>
      {children}
    </div>
  );
}

function Field({
  label,
  htmlFor,
  children,
}: {
  label: string;
  htmlFor: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={htmlFor} className="text-xs text-muted-foreground">
        {label}
      </Label>
      {children}
    </div>
  );
}
