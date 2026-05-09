import { useState } from "react";
import { Edit, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Progress } from "@/components/ui/progress";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { AdminCard } from "@/components/admin/AdminCard";
import { EventStatusBadge } from "@/components/admin/StatusBadge";
import { useEventList, useRounds } from "@/hooks";
import { formatDate, formatTime } from "@/lib/format";

export default function RoundsAdminPage() {
  const [eventFilter, setEventFilter] = useState<string>("ALL");
  const { data: events = [] } = useEventList("ALL");
  const { data: rounds = [] } = useRounds(
    eventFilter === "ALL" ? undefined : Number(eventFilter),
  );

  const titleByEventId = new Map(events.map((e) => [e.id, e.title]));

  return (
    <AdminCard
      title="회차 관리"
      action={
        <Button className="bg-[#FF6B47] hover:bg-[#E5532E]">
          <Plus className="mr-1 h-4 w-4" />
          회차 등록
        </Button>
      }
    >
      <div className="mb-5">
        <Select value={eventFilter} onValueChange={setEventFilter}>
          <SelectTrigger className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">전체 대회</SelectItem>
            {events.map((e) => (
              <SelectItem key={e.id} value={String(e.id)}>
                {e.title}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">ID</TableHead>
            <TableHead>대회</TableHead>
            <TableHead>회차</TableHead>
            <TableHead>대진</TableHead>
            <TableHead>일시</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>판매현황</TableHead>
            <TableHead className="text-right">관리</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rounds.map((r) => {
            const ratio = Math.round((r.soldSeats / r.totalSeats) * 100);
            return (
              <TableRow key={r.id}>
                <TableCell className="font-medium">{r.id}</TableCell>
                <TableCell>{titleByEventId.get(r.eventId)}</TableCell>
                <TableCell className="font-semibold">{r.matchTitle}</TableCell>
                <TableCell>{r.matchUp}</TableCell>
                <TableCell className="text-sm">
                  {formatDate(r.startAt)} {formatTime(r.startAt)}
                </TableCell>
                <TableCell>
                  <EventStatusBadge status={r.status} />
                </TableCell>
                <TableCell className="w-44 text-xs">
                  <div className="flex items-center justify-between">
                    <span>{ratio}%</span>
                    <span className="text-muted-foreground">
                      {r.soldSeats} / {r.totalSeats}석
                    </span>
                  </div>
                  <Progress value={ratio} className="mt-1.5 h-1" />
                </TableCell>
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
            );
          })}
        </TableBody>
      </Table>
    </AdminCard>
  );
}
