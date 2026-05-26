import { useEffect, useState } from "react";
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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useAdminEventDetail,
  useCreateEventMutation,
  useUpdateEventMutation,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import type {
  AdminEventCreateRequest,
  AdminEventUpdateRequest,
  EventStatus,
  SportType,
} from "@/types";

type EventSportType = Exclude<SportType, "ALL">;

const SPORT_OPTIONS: { value: EventSportType; label: string }[] = [
  { value: "LOL", label: "LOL" },
  { value: "VALORANT", label: "VALORANT" },
  { value: "OVERWATCH", label: "OVERWATCH" },
  { value: "TFT", label: "TFT" },
  { value: "PUBG", label: "PUBG" },
];

const STATUS_OPTIONS: { value: EventStatus; label: string }[] = [
  { value: "UPCOMING", label: "예매 예정" },
  { value: "OPEN", label: "예매 진행중" },
  { value: "FINISHED", label: "예매 마감" },
];

interface FormState {
  title: string;
  sportType: EventSportType;
  place: string;
  startDate: string;
  endDate: string;
  maxTicketsPerUser: 1 | 2;
  cancelFee: string;
  status: EventStatus;
  thumbnailUrl: string;
  detailImageUrl: string;
  description: string;
  matchDurationText: string;
  ageRating: string;
  bookingNotice: string;
}

const EMPTY_FORM: FormState = {
  title: "",
  sportType: "LOL",
  place: "",
  startDate: "",
  endDate: "",
  maxTicketsPerUser: 2,
  cancelFee: "1000",
  status: "UPCOMING",
  thumbnailUrl: "",
  detailImageUrl: "",
  description: "",
  matchDurationText: "",
  ageRating: "",
  bookingNotice: "",
};

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  eventId: number | null; // null이면 신규 등록 모드
}

export function EventFormDialog({ open, onOpenChange, eventId }: Props) {
  const isEdit = eventId !== null;
  const [form, setForm] = useState<FormState>(EMPTY_FORM);

  // 수정 모드일 때만 상세 fetch (모달 열려있을 때)
  const detailQuery = useAdminEventDetail(open && isEdit ? eventId : null);

  const createMutation = useCreateEventMutation();
  const updateMutation = useUpdateEventMutation();
  const isPending = createMutation.isPending || updateMutation.isPending;

  // 다이얼로그 상태/상세 데이터에 따라 폼 초기화
  useEffect(() => {
    if (!open) return;

    if (!isEdit) {
      setForm(EMPTY_FORM);
      return;
    }

    // 수정 모드 — 상세 응답이 도착하면 prefill
    if (detailQuery.data) {
      const d = detailQuery.data;
      setForm({
        title: d.title,
        sportType: d.sportType,
        place: d.place,
        startDate: d.startDate,
        endDate: d.endDate,
        maxTicketsPerUser: (d.maxTicketsPerUser === 1 ? 1 : 2),
        cancelFee: String(d.cancelFee),
        status: d.status,
        thumbnailUrl: d.thumbnailUrl ?? "",
        detailImageUrl: d.detailImageUrl ?? "",
        description: d.description ?? "",
        matchDurationText: d.matchDurationText ?? "",
        ageRating: d.ageRating ?? "",
        bookingNotice: d.bookingNotice ?? "",
      });
    }
  }, [open, isEdit, detailQuery.data]);

  const update = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // 사전 검증
    if (!form.title.trim()) return toast.error("대회명을 입력해주세요.");
    if (!form.place.trim()) return toast.error("장소를 입력해주세요.");
    if (!form.startDate || !form.endDate)
      return toast.error("시작일과 종료일을 입력해주세요.");
    if (form.endDate < form.startDate)
      return toast.error("종료일은 시작일 이후여야 합니다.");

    const cancelFeeNum = Number(form.cancelFee);
    if (Number.isNaN(cancelFeeNum) || cancelFeeNum < 0)
      return toast.error("취소 수수료는 0 이상의 숫자여야 합니다.");

    if (isEdit && eventId !== null) {
      const body: AdminEventUpdateRequest = {
        title: form.title.trim(),
        sportType: form.sportType,
        place: form.place.trim(),
        startDate: form.startDate,
        endDate: form.endDate,
        maxTicketsPerUser: form.maxTicketsPerUser,
        cancelFee: cancelFeeNum,
        status: form.status,
        thumbnailUrl: form.thumbnailUrl.trim() || undefined,
        detailImageUrl: form.detailImageUrl.trim() || undefined,
        description: form.description.trim() || undefined,
        matchDurationText: form.matchDurationText.trim() || undefined,
        ageRating: form.ageRating.trim() || undefined,
        bookingNotice: form.bookingNotice.trim() || undefined,
      };
      updateMutation.mutate(
        { eventId, body },
        {
          onSuccess: () => {
            toast.success("대회가 수정되었습니다.");
            onOpenChange(false);
          },
          onError: (err) =>
            toast.error(resolveErrorMessage(err, "대회 수정에 실패했습니다.")),
        },
      );
    } else {
      const body: AdminEventCreateRequest = {
        title: form.title.trim(),
        sportType: form.sportType,
        place: form.place.trim(),
        startDate: form.startDate,
        endDate: form.endDate,
        maxTicketsPerUser: form.maxTicketsPerUser,
        cancelFee: cancelFeeNum,
        thumbnailUrl: form.thumbnailUrl.trim() || undefined,
        detailImageUrl: form.detailImageUrl.trim() || undefined,
        description: form.description.trim() || undefined,
        matchDurationText: form.matchDurationText.trim() || undefined,
        ageRating: form.ageRating.trim() || undefined,
        bookingNotice: form.bookingNotice.trim() || undefined,
      };
      createMutation.mutate(body, {
        onSuccess: () => {
          toast.success("대회가 등록되었습니다.");
          onOpenChange(false);
        },
        onError: (err) =>
          toast.error(resolveErrorMessage(err, "대회 등록에 실패했습니다.")),
      });
    }
  };

  const loadingDetail = isEdit && detailQuery.isLoading;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "대회 수정" : "대회 등록"}</DialogTitle>
        </DialogHeader>

        {loadingDetail ? (
          <p className="py-12 text-center text-sm text-muted-foreground">
            불러오는 중...
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* ── 필수 정보 ── */}
            <Section title="필수 정보">
              <Field label="대회명 *" htmlFor="ev-title">
                <Input
                  id="ev-title"
                  value={form.title}
                  onChange={(e) => update("title", e.target.value)}
                  placeholder="예: LOL 챔피언스 코리아 2026 스프링 결승"
                  maxLength={200}
                  required
                />
              </Field>
              <div className="grid grid-cols-2 gap-3">
                <Field label="종목 *" htmlFor="ev-sport">
                  <Select
                    value={form.sportType}
                    onValueChange={(v) =>
                      update("sportType", v as EventSportType)
                    }
                  >
                    <SelectTrigger id="ev-sport">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {SPORT_OPTIONS.map((opt) => (
                        <SelectItem key={opt.value} value={opt.value}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field label="장소 *" htmlFor="ev-place">
                  <Input
                    id="ev-place"
                    value={form.place}
                    onChange={(e) => update("place", e.target.value)}
                    placeholder="예: LoL Park"
                    maxLength={200}
                    required
                  />
                </Field>
              </div>
            </Section>

            {/* ── 일정 ── */}
            <Section title="일정">
              <div className="grid grid-cols-2 gap-3">
                <Field label="시작일 *" htmlFor="ev-start">
                  <Input
                    id="ev-start"
                    type="date"
                    value={form.startDate}
                    onChange={(e) => update("startDate", e.target.value)}
                    required
                  />
                </Field>
                <Field label="종료일 *" htmlFor="ev-end">
                  <Input
                    id="ev-end"
                    type="date"
                    value={form.endDate}
                    onChange={(e) => update("endDate", e.target.value)}
                    required
                  />
                </Field>
              </div>
            </Section>

            {/* ── 정책 ── */}
            <Section title="정책">
              <div className="grid grid-cols-2 gap-3">
                <Field label="1인 최대 예매 *" htmlFor="ev-max">
                  <Select
                    value={String(form.maxTicketsPerUser)}
                    onValueChange={(v) =>
                      update("maxTicketsPerUser", Number(v) as 1 | 2)
                    }
                  >
                    <SelectTrigger id="ev-max">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="1">1매</SelectItem>
                      <SelectItem value="2">2매</SelectItem>
                    </SelectContent>
                  </Select>
                </Field>
                <Field label="취소 수수료(원) *" htmlFor="ev-fee">
                  <Input
                    id="ev-fee"
                    type="number"
                    min={0}
                    value={form.cancelFee}
                    onChange={(e) => update("cancelFee", e.target.value)}
                    required
                  />
                </Field>
              </div>
              {isEdit && (
                <Field label="상태" htmlFor="ev-status">
                  <Select
                    value={form.status}
                    onValueChange={(v) => update("status", v as EventStatus)}
                  >
                    <SelectTrigger id="ev-status">
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

            {/* ── 이미지 / 부가 정보 ── */}
            <Section title="이미지 · 부가 정보">
              <Field label="썸네일 URL" htmlFor="ev-thumb">
                <Input
                  id="ev-thumb"
                  value={form.thumbnailUrl}
                  onChange={(e) => update("thumbnailUrl", e.target.value)}
                  placeholder="https:// 또는 백엔드 매핑 키 (예: lck_thumb.png)"
                  maxLength={500}
                />
              </Field>
              <Field label="상세 이미지 URL" htmlFor="ev-detailimg">
                <Input
                  id="ev-detailimg"
                  value={form.detailImageUrl}
                  onChange={(e) => update("detailImageUrl", e.target.value)}
                  maxLength={500}
                />
              </Field>
              <Field label="설명" htmlFor="ev-desc">
                <Textarea
                  id="ev-desc"
                  value={form.description}
                  onChange={(e) => update("description", e.target.value)}
                  rows={3}
                />
              </Field>
              <div className="grid grid-cols-2 gap-3">
                <Field label="경기 시간 안내" htmlFor="ev-duration">
                  <Input
                    id="ev-duration"
                    value={form.matchDurationText}
                    onChange={(e) =>
                      update("matchDurationText", e.target.value)
                    }
                    placeholder="예: 약 240분"
                    maxLength={100}
                  />
                </Field>
                <Field label="관람 등급" htmlFor="ev-age">
                  <Input
                    id="ev-age"
                    value={form.ageRating}
                    onChange={(e) => update("ageRating", e.target.value)}
                    placeholder="예: 12세 이용가"
                    maxLength={30}
                  />
                </Field>
              </div>
              <Field label="예매 안내" htmlFor="ev-notice">
                <Textarea
                  id="ev-notice"
                  value={form.bookingNotice}
                  onChange={(e) => update("bookingNotice", e.target.value)}
                  rows={3}
                  maxLength={300}
                />
              </Field>
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
