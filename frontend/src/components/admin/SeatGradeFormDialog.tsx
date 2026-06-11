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
  useCreateSeatGradeMutation,
  useUpdateSeatGradeMutation,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import type { AdminSeatGradeResponse } from "@/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  eventId: number;
  /** null 이면 등록 모드, 있으면 수정 모드 */
  grade: AdminSeatGradeResponse | null;
}

const DEFAULT_COLOR = "#9333EA";

export function SeatGradeFormDialog({
  open,
  onOpenChange,
  eventId,
  grade,
}: Props) {
  const isEdit = grade !== null;

  const [gradeCode, setGradeCode] = useState("");
  const [price, setPrice] = useState("");
  const [colorHex, setColorHex] = useState(DEFAULT_COLOR);

  const createMutation = useCreateSeatGradeMutation(eventId);
  const updateMutation = useUpdateSeatGradeMutation(eventId);
  const isPending = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (!open) return;
    if (isEdit && grade) {
      setGradeCode(grade.gradeCode);
      setPrice(String(grade.price));
      setColorHex(grade.colorHex ?? DEFAULT_COLOR);
    } else {
      setGradeCode("");
      setPrice("");
      setColorHex(DEFAULT_COLOR);
    }
  }, [open, isEdit, grade]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!isEdit && !gradeCode.trim()) {
      return toast.error("등급 코드를 입력해주세요.");
    }

    const priceNum = Number(price);
    if (Number.isNaN(priceNum) || priceNum <= 0) {
      return toast.error("가격은 0보다 큰 숫자여야 합니다.");
    }
    if (!/^#[A-Fa-f0-9]{6}$/.test(colorHex)) {
      return toast.error("색상은 #RRGGBB 형식이어야 합니다.");
    }

    if (isEdit && grade) {
      updateMutation.mutate(
        {
          seatGradeId: grade.seatGradeId,
          body: { price: priceNum, colorHex },
        },
        {
          onSuccess: () => {
            toast.success("좌석 등급이 수정되었습니다.");
            onOpenChange(false);
          },
          onError: (err) =>
            toast.error(resolveErrorMessage(err, "수정에 실패했습니다.")),
        },
      );
    } else {
      createMutation.mutate(
        {
          gradeCode: gradeCode.trim().toUpperCase(),
          price: priceNum,
          colorHex,
        },
        {
          onSuccess: () => {
            toast.success("좌석 등급이 등록되었습니다.");
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
          <DialogTitle>
            {isEdit ? `${grade?.gradeCode}석 수정` : "좌석 등급 등록"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isEdit && (
            <div className="space-y-1.5">
              <Label
                htmlFor="sg-code"
                className="text-xs text-muted-foreground"
              >
                등급 코드 * (예: VIP, R, S, A)
              </Label>
              <Input
                id="sg-code"
                value={gradeCode}
                onChange={(e) => setGradeCode(e.target.value.toUpperCase())}
                placeholder="예: VIP"
                maxLength={20}
                required
              />
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="sg-price" className="text-xs text-muted-foreground">
              가격 (원) *
            </Label>
            <Input
              id="sg-price"
              type="number"
              min={1}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="sg-color" className="text-xs text-muted-foreground">
              색상 *
            </Label>
            <div className="flex items-center gap-2">
              <Input
                id="sg-color"
                type="color"
                value={colorHex}
                onChange={(e) => setColorHex(e.target.value.toUpperCase())}
                className="h-10 w-16 cursor-pointer p-1"
              />
              <Input
                value={colorHex}
                onChange={(e) => setColorHex(e.target.value.toUpperCase())}
                placeholder="#RRGGBB"
                maxLength={7}
                className="flex-1 font-mono"
              />
            </div>
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
