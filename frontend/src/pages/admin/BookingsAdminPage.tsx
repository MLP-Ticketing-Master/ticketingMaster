import { useState } from "react";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
import { BookingStatusBadge } from "@/components/admin/StatusBadge";
import { useAdminBookings } from "@/hooks";
import { formatDate, formatPrice, formatTime } from "@/lib/format";

export default function BookingsAdminPage() {
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("ALL");
  const [page, setPage] = useState(0);

  const { data } = useAdminBookings({ q, status, page });
  const list = data?.content ?? [];

  return (
    <AdminCard title="예매 관리">
      <div className="mb-5 flex gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="예매번호, 고객명, 대회명으로 검색"
            className="pl-10"
          />
        </div>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">전체 상태</SelectItem>
            <SelectItem value="CONFIRMED">예매확정</SelectItem>
            <SelectItem value="PENDING_PAYMENT">결제대기</SelectItem>
            <SelectItem value="CANCELED">취소완료</SelectItem>
            <SelectItem value="WATCHED">관람완료</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>예매번호</TableHead>
            <TableHead>고객정보</TableHead>
            <TableHead>대회</TableHead>
            <TableHead>회차정보</TableHead>
            <TableHead>좌석</TableHead>
            <TableHead>결제</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>예매일시</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {list.map((b) => (
            <TableRow key={b.id}>
              <TableCell className="font-medium">{b.bookingNo}</TableCell>
              <TableCell>
                <p className="font-semibold">{b.customerName}</p>
                <p className="text-xs text-muted-foreground">
                  {b.customerEmail}
                </p>
              </TableCell>
              <TableCell className="text-sm">{b.eventTitle}</TableCell>
              <TableCell className="text-sm">
                {formatDate(b.startAt)} {formatTime(b.startAt)}
                <br />- {b.roundLabel}
              </TableCell>
              <TableCell className="text-xs">
                {b.seatLabels.join(", ")}
              </TableCell>
              <TableCell>
                <p className="font-bold">{formatPrice(b.amount)}</p>
                <p className="text-xs text-muted-foreground">
                  {b.paymentMethod}
                </p>
              </TableCell>
              <TableCell>
                <BookingStatusBadge status={b.status} />
              </TableCell>
              <TableCell className="text-sm">
                {formatDate(b.bookedAt, true)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <div className="mt-5 flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          총 {data?.totalElements ?? 0}건의 예매 내역
        </p>
        <div className="flex gap-2">
          <Button
            variant="outline"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            이전
          </Button>
          {Array.from({ length: data?.totalPages ?? 1 }, (_, i) => (
            <Button
              key={i}
              variant={page === i ? "default" : "outline"}
              onClick={() => setPage(i)}
              className={page === i ? "bg-[#FF6B47] hover:bg-[#E5532E]" : ""}
            >
              {i + 1}
            </Button>
          ))}
          <Button
            variant="outline"
            disabled={page >= (data?.totalPages ?? 1) - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </Button>
        </div>
      </div>
    </AdminCard>
  );
}
