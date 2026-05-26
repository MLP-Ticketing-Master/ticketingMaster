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
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useDeleteSeatMutation,
  useUpdateSeatMutation,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import type {
  AdminSeatGradeResponse,
  AdminSeatResponse,
  AdminSectionResponse,
} from "@/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  matchId: number;
  seat: AdminSeatResponse | null;
  sections: AdminSectionResponse[];
  grades: AdminSeatGradeResponse[];
}

export function SeatFormDialog({
  open,
  onOpenChange,
  matchId,
  seat,
  sections,
  grades,
}: Props) {
  const [sectionId, setSectionId] = useState("");
  const [gradeId, setGradeId] = useState("");

  const updateMutation = useUpdateSeatMutation(matchId);
  const deleteMutation = useDeleteSeatMutation(matchId);
  const isPending = updateMutation.isPending || deleteMutation.isPending;

  useEffect(() => {
    if (!open || !seat) return;
    // seat 응답엔 sectionId/seatGradeId 없이 name/code 만 있으므로 매핑
    const s = sections.find((x) => x.name === seat.sectionName);
    const g = grades.find((x) => x.gradeCode === seat.gradeCode);
    setSectionId(s ? String(s.sectionId) : "");
    setGradeId(g ? String(g.seatGradeId) : "");
  }, [open, seat, sections, grades]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!seat) return;

    if (!sectionId) return toast.error("구역을 선택해주세요.");
    if (!gradeId) return toast.error("등급을 선택해주세요.");

    updateMutation.mutate(
      {
        seatId: seat.seatId,
        body: {
          sectionId: Number(sectionId),
          seatGradeId: Number(gradeId),
        },
      },
      {
        onSuccess: () => {
          toast.success("좌석이 수정되었습니다.");
          onOpenChange(false);
        },
        onError: (err) =>
          toast.error(resolveErrorMessage(err, "좌석 수정에 실패했습니다.")),
      },
    );
  };

  const handleDelete = () => {
    if (!seat) return;
    if (!confirm(`'${seat.seatCode}' 좌석을 삭제하시겠습니까?`)) return;
    deleteMutation.mutate(seat.seatId, {
      onSuccess: () => {
        toast.success("좌석이 삭제되었습니다.");
        onOpenChange(false);
      },
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "좌석 삭제에 실패했습니다.")),
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {seat ? `${seat.seatCode} 좌석 관리` : "좌석 관리"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
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
                  <SelectItem key={g.seatGradeId} value={String(g.seatGradeId)}>
                    {g.gradeCode}석
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <DialogFooter className="!justify-between">
            <Button
              type="button"
              variant="outline"
              onClick={handleDelete}
              disabled={isPending}
              className="border-red-200 text-red-500 hover:bg-red-50 hover:text-red-600"
            >
              삭제
            </Button>
            <div className="flex gap-2">
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
                {updateMutation.isPending ? "처리 중..." : "수정"}
              </Button>
            </div>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
