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
import {
  useCreateTeamMutation,
  useUpdateTeamMutation,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import type { Team, TeamSportType } from "@/types";

const SPORT_OPTIONS: { value: TeamSportType; label: string }[] = [
  { value: "LOL", label: "LOL" },
  { value: "VALORANT", label: "VALORANT" },
  { value: "OVERWATCH", label: "OVERWATCH" },
  { value: "TFT", label: "TFT" },
  { value: "PUBG", label: "PUBG" },
];

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  team: Team | null; // null이면 신규 등록 모드
}

export function TeamFormDialog({ open, onOpenChange, team }: Props) {
  const isEdit = team !== null;
  const [name, setName] = useState("");
  const [sportType, setSportType] = useState<TeamSportType>("LOL");
  const [logoImageUrl, setLogoImageUrl] = useState("");

  const createMutation = useCreateTeamMutation();
  const updateMutation = useUpdateTeamMutation();
  const isPending = createMutation.isPending || updateMutation.isPending;

  // 다이얼로그가 열릴 때마다 폼 초기화 (수정 시 기존 값, 등록 시 빈 값)
  useEffect(() => {
    if (open) {
      setName(team?.name ?? "");
      setSportType(team?.sportType ?? "LOL");
      setLogoImageUrl(team?.logoImageUrl ?? "");
    }
  }, [open, team]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedName = name.trim();
    if (!trimmedName) {
      toast.error("팀명을 입력해주세요.");
      return;
    }

    const body = {
      name: trimmedName,
      sportType,
      logoImageUrl: logoImageUrl.trim() || undefined,
    };

    if (isEdit && team) {
      updateMutation.mutate(
        { teamId: team.teamId, body },
        {
          onSuccess: () => {
            toast.success("팀이 수정되었습니다.");
            onOpenChange(false);
          },
          onError: (err) =>
            toast.error(resolveErrorMessage(err, "팀 수정에 실패했습니다.")),
        },
      );
    } else {
      createMutation.mutate(body, {
        onSuccess: () => {
          toast.success("팀이 등록되었습니다.");
          onOpenChange(false);
        },
        onError: (err) =>
          toast.error(resolveErrorMessage(err, "팀 등록에 실패했습니다.")),
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? "팀 수정" : "팀 등록"}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="team-name">팀명 *</Label>
            <Input
              id="team-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: T1, Gen.G"
              maxLength={100}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="team-sport">종목 *</Label>
            <Select
              value={sportType}
              onValueChange={(v) => setSportType(v as TeamSportType)}
            >
              <SelectTrigger id="team-sport">
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
          </div>

          <div className="space-y-2">
            <Label htmlFor="team-logo">로고 이미지 URL</Label>
            <Input
              id="team-logo"
              value={logoImageUrl}
              onChange={(e) => setLogoImageUrl(e.target.value)}
              placeholder="https://..."
              maxLength={500}
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
