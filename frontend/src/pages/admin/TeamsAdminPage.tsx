import { useState } from "react";
import { Edit, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { AdminCard } from "@/components/admin/AdminCard";
import { GameBadge } from "@/components/admin/GameBadge";
import { TeamFormDialog } from "@/components/admin/TeamFormDialog";
import { useDeleteTeamMutation, useTeams } from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { resolveTeamLogo } from "@/lib/teamImages";
import type { GameType, Team } from "@/types";

export default function TeamsAdminPage() {
  const [game, setGame] = useState<GameType>("ALL");
  const { data: teams = [], isLoading, isError } = useTeams(game);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingTeam, setEditingTeam] = useState<Team | null>(null);
  const deleteMutation = useDeleteTeamMutation();

  const handleCreate = () => {
    setEditingTeam(null);
    setDialogOpen(true);
  };

  const handleEdit = (team: Team) => {
    setEditingTeam(team);
    setDialogOpen(true);
  };

  const handleDelete = (team: Team) => {
    if (!confirm(`'${team.name}' 팀을 삭제하시겠습니까?`)) return;
    deleteMutation.mutate(team.teamId, {
      onSuccess: () => toast.success("팀이 삭제되었습니다."),
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "팀 삭제에 실패했습니다.")),
    });
  };

  return (
    <>
      <AdminCard
        title="팀 관리"
        action={
          <Button
            onClick={handleCreate}
            className="bg-[#054EFD] hover:bg-[#3C76FE]"
          >
            <Plus className="mr-1 h-4 w-4" />팀 등록
          </Button>
        }
      >
        <div className="mb-5">
          <Select value={game} onValueChange={(v) => setGame(v as GameType)}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">전체 종목</SelectItem>
              <SelectItem value="LOL">LOL</SelectItem>
              <SelectItem value="VALORANT">VALORANT</SelectItem>
              <SelectItem value="OVERWATCH">OVERWATCH</SelectItem>
              <SelectItem value="TFT">TFT</SelectItem>
              <SelectItem value="PUBG">PUBG</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {isLoading && (
          <p className="py-12 text-center text-sm text-muted-foreground">
            불러오는 중...
          </p>
        )}

        {isError && (
          <p className="py-12 text-center text-sm text-red-500">
            팀 목록을 불러오는 데 실패했습니다.
          </p>
        )}

        {!isLoading && !isError && teams.length === 0 && (
          <p className="py-12 text-center text-sm text-muted-foreground">
            등록된 팀이 없습니다.
          </p>
        )}

        {!isLoading && !isError && teams.length > 0 && (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {teams.map((team) => (
              <TeamCard
                key={team.teamId}
                team={team}
                onEdit={() => handleEdit(team)}
                onDelete={() => handleDelete(team)}
                isDeleting={deleteMutation.isPending}
              />
            ))}
          </div>
        )}
      </AdminCard>

      <TeamFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        team={editingTeam}
      />
    </>
  );
}

interface TeamCardProps {
  team: Team;
  onEdit: () => void;
  onDelete: () => void;
  isDeleting: boolean;
}

function TeamCard({ team, onEdit, onDelete, isDeleting }: TeamCardProps) {
  const logoSrc = resolveTeamLogo(team.logoImageUrl);
  return (
    <Card className="space-y-3 p-5">
      <div className="flex items-center gap-3">
        {logoSrc ? (
          <img
            src={logoSrc}
            alt={team.name}
            className="h-14 w-14 rounded-lg bg-gray-100 object-contain"
          />
        ) : (
          <div className="flex h-14 w-14 items-center justify-center rounded-lg bg-gray-100 text-xl font-bold text-gray-400">
            {team.name.charAt(0)}
          </div>
        )}
        <div>
          <p className="font-bold">{team.name}</p>
          <GameBadge game={team.sportType} />
        </div>
      </div>

      <div className="flex gap-2">
        <Button variant="outline" className="flex-1" onClick={onEdit}>
          <Edit className="mr-1 h-4 w-4" />
          수정
        </Button>
        <Button
          size="icon"
          variant="outline"
          onClick={onDelete}
          disabled={isDeleting}
          className="border-red-300 text-red-500 hover:bg-red-50"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </Card>
  );
}
