import { Badge } from "@/components/ui/badge";
import type { BookingStatus, EventStatus } from "@/types";

const EVENT_STATUS: Record<EventStatus, { label: string; cls: string }> = {
  OPEN: { label: "예매 진행중", cls: "bg-emerald-100 text-emerald-700" },
  UPCOMING: { label: "예매 예정", cls: "bg-blue-100 text-blue-700" },
  FINISHED: { label: "예매 마감", cls: "bg-gray-100 text-gray-700" },
};

const BOOKING_STATUS: Record<BookingStatus, { label: string; cls: string }> = {
  CONFIRMED: { label: "예매확정", cls: "bg-emerald-100 text-emerald-700" },
  CANCELED: { label: "취소완료", cls: "bg-rose-100 text-rose-700" },
  PENDING_PAYMENT: { label: "결제대기", cls: "bg-yellow-100 text-yellow-700" },
  WATCHED: { label: "관람완료", cls: "bg-gray-100 text-gray-700" },
};

export function EventStatusBadge({ status }: { status: EventStatus }) {
  const { label, cls } = EVENT_STATUS[status];
  return <Badge className={cls}>{label}</Badge>;
}

export function BookingStatusBadge({ status }: { status: BookingStatus }) {
  const { label, cls } = BOOKING_STATUS[status];
  return <Badge className={cls}>{label}</Badge>;
}
