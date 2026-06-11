import { useState } from "react";
import { Edit, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
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
import { EventFormDialog } from "@/components/admin/EventFormDialog";
import { EventStatusBadge } from "@/components/admin/StatusBadge";
import { GameBadge } from "@/components/admin/GameBadge";
import { useAdminEvents, useDeleteEventMutation } from "@/hooks";
import { resolveErrorMessage } from "@/lib/error";
import { formatDate } from "@/lib/format";
import type { AdminEventListResponse } from "@/types";

export default function EventsAdminPage() {
  const { data, isLoading, isError } = useAdminEvents();
  const events = data?.content ?? [];

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const deleteMutation = useDeleteEventMutation();

  const openCreate = () => {
    setEditingId(null);
    setDialogOpen(true);
  };

  const openEdit = (event: AdminEventListResponse) => {
    setEditingId(event.eventId);
    setDialogOpen(true);
  };

  const handleDelete = (event: AdminEventListResponse) => {
    if (!confirm(`'${event.title}' 대회를 삭제하시겠습니까?`)) return;
    deleteMutation.mutate(event.eventId, {
      onSuccess: () => toast.success("대회가 삭제되었습니다."),
      onError: (err) =>
        toast.error(resolveErrorMessage(err, "대회 삭제에 실패했습니다.")),
    });
  };

  return (
    <>
      <AdminCard
        title="대회 관리"
        action={
          <Button
            onClick={openCreate}
            className="bg-[#054EFD] hover:bg-[#3C76FE]"
          >
            <Plus className="mr-1 h-4 w-4" />
            대회 등록
          </Button>
        }
      >
        {isLoading && (
          <p className="py-12 text-center text-sm text-muted-foreground">
            불러오는 중...
          </p>
        )}

        {isError && (
          <p className="py-12 text-center text-sm text-red-500">
            대회 목록을 불러오는 데 실패했습니다.
          </p>
        )}

        {!isLoading && !isError && events.length === 0 && (
          <p className="py-12 text-center text-sm text-muted-foreground">
            등록된 대회가 없습니다.
          </p>
        )}

        {!isLoading && !isError && events.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>대회명</TableHead>
                <TableHead className="w-28">종목</TableHead>
                <TableHead className="w-32">장소</TableHead>
                <TableHead className="w-44">기간</TableHead>
                <TableHead className="w-28">상태</TableHead>
                <TableHead className="w-24 text-right">관리</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {events.map((event) => (
                <TableRow key={event.eventId}>
                  <TableCell className="font-semibold">{event.title}</TableCell>
                  <TableCell>
                    <GameBadge game={event.sportType} />
                  </TableCell>
                  <TableCell className="text-sm">{event.place}</TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {formatDate(event.startDate)} ~<br />
                    {formatDate(event.endDate)}
                  </TableCell>
                  <TableCell>
                    <EventStatusBadge status={event.status} />
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => openEdit(event)}
                        className="h-8 w-8 text-gray-500 hover:text-gray-700"
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => handleDelete(event)}
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

      <EventFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        eventId={editingId}
      />
    </>
  );
}
