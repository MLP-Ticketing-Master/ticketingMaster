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
  useCreateSectionMutation,
  useUpdateSectionMutation,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import type { AdminSectionResponse } from "@/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  eventId: number;
  /** null 이면 등록 모드, 있으면 수정 모드 */
  section: AdminSectionResponse | null;
  /** 등록 시 기본 displayOrder (목록 마지막+1) */
  defaultDisplayOrder?: number;
}

export function SectionFormDialog({
  open,
  onOpenChange,
  eventId,
  section,
  defaultDisplayOrder,
}: Props) {
  const isEdit = section !== null;

  const [name, setName] = useState("");
  const [displayOrder, setDisplayOrder] = useState("1");
  const [description, setDescription] = useState("");

  const createMutation = useCreateSectionMutation(eventId);
  const updateMutation = useUpdateSectionMutation(eventId);
  const isPending = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (!open) return;
    if (isEdit && section) {
      setName(section.name);
      setDisplayOrder(String(section.displayOrder));
      setDescription(section.description ?? "");
    } else {
      setName("");
      setDisplayOrder(String(defaultDisplayOrder ?? 1));
      setDescription("");
    }
  }, [open, isEdit, section, defaultDisplayOrder]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) return toast.error("구역명을 입력해주세요.");
    const orderNum = Number(displayOrder);
    if (Number.isNaN(orderNum) || orderNum <= 0) {
      return toast.error("정렬 순서는 0보다 큰 숫자여야 합니다.");
    }

    if (isEdit && section) {
      updateMutation.mutate(
        {
          sectionId: section.sectionId,
          body: {
            name: name.trim(),
            displayOrder: orderNum,
            description: description.trim() || undefined,
          },
        },
        {
          onSuccess: () => {
            toast.success("구역이 수정되었습니다.");
            onOpenChange(false);
          },
          onError: (err) =>
            toast.error(resolveErrorMessage(err, "수정에 실패했습니다.")),
        },
      );
    } else {
      createMutation.mutate(
        {
          name: name.trim(),
          displayOrder: orderNum,
          description: description.trim() || undefined,
        },
        {
          onSuccess: () => {
            toast.success("구역이 등록되었습니다.");
            onOpenChange(false);
          },
          onError: (err) =>
            toast.error(resolveErrorMessage(err, "등록에 실패했습니다.")),
        },
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? "구역 수정" : "구역 등록"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="sec-name" className="text-xs text-muted-foreground">
              구역명 *
            </Label>
            <Input
              id="sec-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 좌측 구역"
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label
              htmlFor="sec-order"
              className="text-xs text-muted-foreground"
            >
              정렬 순서 *
            </Label>
            <Input
              id="sec-order"
              type="number"
              min={1}
              value={displayOrder}
              onChange={(e) => setDisplayOrder(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="sec-desc" className="text-xs text-muted-foreground">
              설명
            </Label>
            <Textarea
              id="sec-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
            />
          </div>

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
      </DialogContent>
    </Dialog>
  );
}
