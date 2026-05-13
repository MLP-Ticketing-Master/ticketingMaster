import { useState } from "react";
import { Edit, Plus, Trash2 } from "lucide-react";
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
import { useTeams } from "@/hooks";
import { formatDate } from "@/lib/format";
import type { GameType, Team } from "@/types";

export default function TeamsAdminPage() {
  const [game, setGame] = useState<GameType>("ALL");
  const { data: teams = [] } = useTeams(game);

  return (
    <AdminCard
      title="팀 관리"
      action={
        <Button className="bg-[#054EFD] hover:bg-[#3C76FE]">
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
          </SelectContent>
        </Select>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {teams.map((team) => (
          <TeamCard key={team.id} team={team} />
        ))}
      </div>
    </AdminCard>
  );
}

function TeamCard({ team }: { team: Team }) {
  return (
    <Card className="space-y-3 p-5">
      <div className="flex items-center gap-3">
        <img
          src={team.logoUrl}
          alt={team.name}
          className="h-14 w-14 rounded-lg bg-gray-100 object-cover"
        />
        <div>
          <p className="font-bold">{team.name}</p>
          <GameBadge game={team.game} />
        </div>
      </div>

      <dl className="space-y-1.5 text-sm">
        <Row label="총 경기수" value={`${team.totalMatches}경기`} />
        <Row label="등록일" value={formatDate(team.registeredAt)} />
      </dl>

      <div className="flex gap-2">
        <Button variant="outline" className="flex-1">
          <Edit className="mr-1 h-4 w-4" />
          수정
        </Button>
        <Button
          size="icon"
          variant="outline"
          className="border-red-300 text-red-500 hover:bg-red-50"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </Card>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-semibold">{value}</dd>
    </div>
  );
}
