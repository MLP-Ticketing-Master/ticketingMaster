import { useMemo, useState } from "react";
import { Edit, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { AdminCard } from "@/components/admin/AdminCard";
import { MatchFormDialog } from "@/components/admin/MatchFormDialog";
import { MatchStatusBadge } from "@/components/admin/StatusBadge";
import {
  useAdminEvents,
  useAdminMatches,
  useDeleteMatchMutation,
  useTeams,
} from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { formatDate, formatTime } from "@/lib/format";
import type { AdminMatchResponse } from "@/types";

const ALL = "ALL";

export default function MatchesAdminPage() {
  const [eventFilter, setEventFilter] = useState<string>(ALL);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const eventsQuery = useAdminEvents();
  const events = eventsQuery.data?.content ?? [];

  const teamsQuery = useTeams("ALL");
  const teams = teamsQuery.data ?? [];

  const selectedEventId = eventFilter === ALL ? undefined : Number(eventFilter);
  const matchesQuery = useAdminMatches(selectedEventId);
  const matches = matchesQuery.data?.content ?? [];

  const deleteMutation = useDeleteMatchMutation();

  // eventId/teamId 룩업 맵
  const eventTitleMap = useMemo(() => {
    const map = new Map<number, string>();
    events.forEach((e) => map.set(e.eventId, e.title));
    return map;
  }, [events]);

  const teamNameMap = useMemo(() => {
    const map = new Map<number, string>();
    teams.forEach((t) => map.set(t.teamId, t.name));
    return map;
  }, [teams]);

  const openCreate = () => {
    setEditingId(null);
    setDialogOpen(true);
  };

  const openEdit = (m: AdminMatchResponse) => {
    setEditingId(m.id);
    setDialogOpen(true);
  };

  const handleDelete = (m: AdminMatchResponse) => {
    const label = `${eventTitleMap.get(m.eventId) ?? "대회"} - ${m.roundLabel}`;
    if (!confirm(`'${label}' 회차를 삭제하시겠습니까?`)) return;
    deleteMutation.mutate(m.id, {
      onSuccess: () => toast.success("회차가 삭제되었습니다."),
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "회차 삭제에 실패했습니다.")),
    });
  };

  const renderMatchup = (m: AdminMatchResponse) => {
    const home = m.homeTeamId ? teamNameMap.get(m.homeTeamId) : null;
    const away = m.awayTeamId ? teamNameMap.get(m.awayTeamId) : null;
    if (!home && !away) {
      return <span className="text-muted-foreground">대진 미정</span>;
    }
    return (
      <span>
        {home ?? "-"} <span className="text-muted-foreground">vs</span>{" "}
        {away ?? "-"}
      </span>
    );
  };

  const isLoading = matchesQuery.isLoading || eventsQuery.isLoading;
  const isError = matchesQuery.isError;

  return (
    <>
      <AdminCard
        title="회차 관리"
        action={
          <Button
            onClick={openCreate}
            className="bg-[#054EFD] hover:bg-[#3C76FE]"
            disabled={events.length === 0}
          >
            <Plus className="mr-1 h-4 w-4" />
            회차 등록
          </Button>
        }
      >
        <div className="mb-4 w-60">
          <Select value={eventFilter} onValueChange={setEventFilter}>
            <SelectTrigger>
              <SelectValue placeholder="전체 대회" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>전체 대회</SelectItem>
              {events.map((e) => (
                <SelectItem key={e.eventId} value={String(e.eventId)}>
                  {e.title}
                </SelectItem>
              ))}
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
            회차 목록을 불러오는 데 실패했습니다.
          </p>
        )}

        {!isLoading && !isError && matches.length === 0 && (
          <p className="py-12 text-center text-sm text-muted-foreground">
            등록된 회차가 없습니다.
          </p>
        )}

        {!isLoading && !isError && matches.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>대회</TableHead>
                <TableHead className="w-28">회차</TableHead>
                <TableHead className="w-44">대진</TableHead>
                <TableHead className="w-36">일시</TableHead>
                <TableHead className="w-20">상태</TableHead>
                <TableHead className="w-24 text-right">관리</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {matches.map((m) => (
                <TableRow key={m.id}>
                  <TableCell className="text-sm">
                    {eventTitleMap.get(m.eventId) ?? `#${m.eventId}`}
                  </TableCell>
                  <TableCell className="font-semibold">
                    {m.roundLabel}
                  </TableCell>
                  <TableCell className="text-sm">{renderMatchup(m)}</TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {formatDate(m.startAt)}
                    <br />
                    {formatTime(m.startAt)}
                  </TableCell>
                  <TableCell>
                    <MatchStatusBadge status={m.status} />
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => openEdit(m)}
                        className="h-8 w-8 text-gray-500 hover:text-gray-700"
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => handleDelete(m)}
                        disabled={deleteMutation.isPending}
                        className="h-8 w-8 text-red-500 hover:bg-red-50 hover:text-red-600"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </AdminCard>

      <MatchFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        matchId={editingId}
      />
    </>
  );
}
