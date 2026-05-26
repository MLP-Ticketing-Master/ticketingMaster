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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useBulkCreateSeatsMutation } from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import type {
  AdminSeatBulkCreateRequest,
  AdminSeatGradeResponse,
  AdminSectionResponse,
} from "@/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  matchId: number;
  sections: AdminSectionResponse[];
  grades: AdminSeatGradeResponse[];
}

export function SeatBulkFormDialog({
  open,
  onOpenChange,
  matchId,
  sections,
  grades,
}: Props) {
  const [sectionId, setSectionId] = useState("");
  const [gradeId, setGradeId] = useState("");
  const [rowStart, setRowStart] = useState("A");
  const [rowEnd, setRowEnd] = useState("A");
  const [seatStart, setSeatStart] = useState("1");
  const [seatEnd, setSeatEnd] = useState("12");

  const mutation = useBulkCreateSeatsMutation(matchId);

  useEffect(() => {
    if (!open) return;
    setSectionId(sections[0] ? String(sections[0].sectionId) : "");
    setGradeId(grades[0] ? String(grades[0].seatGradeId) : "");
    setRowStart("A");
    setRowEnd("A");
    setSeatStart("1");
    setSeatEnd("12");
  }, [open, sections, grades]);

  const startNum = Number(seatStart);
  const endNum = Number(seatEnd);
  const normalizedRowStart = rowStart.trim().toUpperCase();
  const normalizedRowEnd = rowEnd.trim().toUpperCase();

  // 단일 알파벳만 허용 (A~Z)
  const isValidRowLetter = (s: string) => /^[A-Z]$/.test(s);
  const rowStartCode = isValidRowLetter(normalizedRowStart)
    ? normalizedRowStart.charCodeAt(0)
    : null;
  const rowEndCode = isValidRowLetter(normalizedRowEnd)
    ? normalizedRowEnd.charCodeAt(0)
    : null;

  const rowCount =
    rowStartCode !== null && rowEndCode !== null && rowEndCode >= rowStartCode
      ? rowEndCode - rowStartCode + 1
      : 0;
  const seatCount =
    Number.isFinite(startNum) && Number.isFinite(endNum) && endNum >= startNum
      ? endNum - startNum + 1
      : 0;
  const totalCount = rowCount * seatCount;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!sectionId) return toast.error("구역을 선택해주세요.");
    if (!gradeId) return toast.error("등급을 선택해주세요.");
    if (rowStartCode === null || rowEndCode === null) {
      return toast.error("행 라벨은 A~Z 한 글자여야 합니다.");
    }
    if (rowEndCode < rowStartCode) {
      return toast.error("행 라벨 범위가 올바르지 않습니다.");
    }
    if (
      !Number.isFinite(startNum) ||
      !Number.isFinite(endNum) ||
      startNum <= 0 ||
      endNum < startNum
    ) {
      return toast.error("좌석 번호 범위가 올바르지 않습니다.");
    }
    if (totalCount > 5000) {
      return toast.error("한 번에 최대 5000석까지 등록 가능합니다.");
    }

    const seats = [];
    for (let r = rowStartCode; r <= rowEndCode; r++) {
      const rowLabel = String.fromCharCode(r);
      for (let n = startNum; n <= endNum; n++) {
        seats.push({
          sectionId: Number(sectionId),
          seatGradeId: Number(gradeId),
          rowLabel,
          seatNo: n,
        });
      }
    }
    const body: AdminSeatBulkCreateRequest = { seats };

    mutation.mutate(body, {
      onSuccess: (created) => {
        toast.success(`${created.length}석이 등록되었습니다.`);
        onOpenChange(false);
      },
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "좌석 등록에 실패했습니다.")),
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>좌석 일괄 등록</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">구역 *</Label>
              <Select value={sectionId} onValueChange={setSectionId}>
                <SelectTrigger>
                  <SelectValue placeholder="구역 선택" />
                </SelectTrigger>
                <SelectContent>
                  {sections.map((s) => (
                    <SelectItem key={s.sectionId} value={String(s.sectionId)}>
                      {s.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">등급 *</Label>
              <Select value={gradeId} onValueChange={setGradeId}>
                <SelectTrigger>
                  <SelectValue placeholder="등급 선택" />
                </SelectTrigger>
                <SelectContent>
                  {grades.map((g) => (
                    <SelectItem
                      key={g.seatGradeId}
                      value={String(g.seatGradeId)}
                    >
                      {g.gradeCode}석
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label
                htmlFor="sb-rowstart"
                className="text-xs text-muted-foreground"
              >
                시작 행 * (예: A)
              </Label>
              <Input
                id="sb-rowstart"
                value={rowStart}
                onChange={(e) => setRowStart(e.target.value.toUpperCase())}
                maxLength={1}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label
                htmlFor="sb-rowend"
                className="text-xs text-muted-foreground"
              >
                종료 행 *
              </Label>
              <Input
                id="sb-rowend"
                value={rowEnd}
                onChange={(e) => setRowEnd(e.target.value.toUpperCase())}
                maxLength={1}
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label
                htmlFor="sb-start"
                className="text-xs text-muted-foreground"
              >
                시작 번호 *
              </Label>
              <Input
                id="sb-start"
                type="number"
                min={1}
                value={seatStart}
                onChange={(e) => setSeatStart(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label
                htmlFor="sb-end"
                className="text-xs text-muted-foreground"
              >
                종료 번호 *
              </Label>
              <Input
                id="sb-end"
                type="number"
                min={1}
                value={seatEnd}
                onChange={(e) => setSeatEnd(e.target.value)}
                required
              />
            </div>
          </div>

          <p className="rounded-md bg-gray-50 px-3 py-2 text-xs text-muted-foreground">
            {totalCount > 0
              ? `${normalizedRowStart}-${seatStart} ~ ${normalizedRowEnd}-${seatEnd} (${rowCount}행 × ${seatCount}번 = ${totalCount}석 등록)`
              : "범위가 올바르지 않습니다."}
          </p>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={mutation.isPending}
            >
              취소
            </Button>
            <Button
              type="submit"
              disabled={mutation.isPending || totalCount === 0}
              className="bg-[#054EFD] hover:bg-[#3C76FE]"
            >
              {mutation.isPending ? "처리 중..." : `${totalCount}석 등록`}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
