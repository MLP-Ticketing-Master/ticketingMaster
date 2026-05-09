import { Edit, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { AdminCard } from "@/components/admin/AdminCard";
import { GameBadge } from "@/components/admin/GameBadge";
import { EventStatusBadge } from "@/components/admin/StatusBadge";
import { useEventList, useRounds } from "@/hooks";
import { formatDateRange } from "@/lib/format";

export default function EventsAdminPage() {
  const { data: events = [] } = useEventList("ALL");
  const { data: rounds = [] } = useRounds();

  const roundCountByEvent = rounds.reduce<Record<number, number>>((acc, r) => {
    acc[r.eventId] = (acc[r.eventId] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <AdminCard
      title="대회 관리"
      action={
        <Button className="bg-[#FF6B47] hover:bg-[#E5532E]">
          <Plus className="mr-1 h-4 w-4" />
          대회 등록
        </Button>
      }
    >
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">ID</TableHead>
            <TableHead>대회명</TableHead>
            <TableHead>종목</TableHead>
            <TableHead>장소</TableHead>
            <TableHead>기간</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>회차 수</TableHead>
            <TableHead className="text-right">관리</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {events.map((event) => (
            <TableRow key={event.id}>
              <TableCell className="font-medium">{event.id}</TableCell>
              <TableCell className="font-semibold">{event.title}</TableCell>
              <TableCell>
                <GameBadge game={event.game} />
              </TableCell>
              <TableCell>{event.venue}</TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {formatDateRange(event.startDate, event.endDate)}
              </TableCell>
              <TableCell>
                <EventStatusBadge status={event.status} />
              </TableCell>
              <TableCell>{roundCountByEvent[event.id] ?? 0}회차</TableCell>
              <TableCell>
                <div className="flex justify-end gap-2">
                  <Button size="icon" variant="outline">
                    <Edit className="h-4 w-4" />
                  </Button>
                  <Button
                    size="icon"
                    variant="outline"
                    className="border-red-300 text-red-500 hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminCard>
  );
}
