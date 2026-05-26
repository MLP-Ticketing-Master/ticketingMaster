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
    <AdminCard title="예매 관리" headerClassName="mb-4">
      {/* 검색 + 상태 필터 — 동일한 높이/스타일 */}
      <div className="mb-6 flex items-stretch gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="예매번호, 고객명, 대회명으로 검색"
            className="h-11 rounded-xl border-input pl-11 text-sm"
          />
        </div>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="!h-11 w-40 rounded-xl border-input text-sm">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">전체 상태</SelectItem>
            <SelectItem value="CONFIRMED">예매확정</SelectItem>
            <SelectItem value="PENDING">결제대기</SelectItem>
            <SelectItem value="CANCELED">취소완료</SelectItem>
            <SelectItem value="EXPIRED">만료</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="py-3 text-sm font-bold text-gray-700">
              예매번호
            </TableHead>
            <TableHead className="py-3 text-sm font-bold text-gray-700">
              고객정보
            </TableHead>
            <TableHead className="py-3 text-sm font-bold text-gray-700">
              대회
            </TableHead>
            <TableHead className="w-32 py-3 text-sm font-bold text-gray-700">
              회차정보
            </TableHead>
            <TableHead className="w-24 py-3 text-sm font-bold text-gray-700">
              좌석
            </TableHead>
            <TableHead className="w-28 py-3 text-sm font-bold text-gray-700">
              결제
            </TableHead>
            <TableHead className="w-16 py-3 text-sm font-bold text-gray-700">
              상태
            </TableHead>
            <TableHead className="w-28 py-3 text-sm font-bold text-gray-700">
              예매일시
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {list.map((b) => (
            <TableRow key={b.id} className="border-b">
              <TableCell className="py-5 text-sm font-medium tabular-nums">
                {b.bookingNo}
              </TableCell>
              <TableCell className="py-5">
                <p className="font-bold">{b.customerName}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {b.customerEmail}
                </p>
              </TableCell>
              <TableCell className="py-5 text-sm">{b.eventTitle}</TableCell>
              <TableCell className="py-5 text-xs leading-relaxed text-muted-foreground">
                {formatDate(b.startAt)} {formatTime(b.startAt)}
                <br />- {b.roundLabel}
              </TableCell>
              <TableCell className="py-5 text-xs leading-relaxed">
                {b.seatLabels.join(", ")}
              </TableCell>
              <TableCell className="py-5">
                <p className="font-bold tabular-nums">{formatPrice(b.amount)}</p>
              </TableCell>
              <TableCell className="py-5">
                <BookingStatusBadge status={b.status} />
              </TableCell>
              <TableCell className="py-5 text-xs leading-relaxed text-muted-foreground">
                {formatDate(b.bookedAt)}
                <br />
                {formatTime(b.bookedAt)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {/* 페이지네이션 */}
      <div className="mt-6 flex items-center justify-between">
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
              className={
                page === i
                  ? "bg-[#054EFD] hover:bg-[#3C76FE]"
                  : ""
              }
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
