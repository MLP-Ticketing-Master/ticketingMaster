import { Calendar, MapPin } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import type { BookingItem } from "@/types";

interface Props {
  booking: BookingItem;
  onCancel?: (id: number) => void;
  onDetail?: (id: number) => void;
}

const STATUS_LABEL: Record<BookingItem["status"], string> = {
  CONFIRMED: "예매완료",
  CANCELED: "예매취소",
  PENDING_PAYMENT: "결제대기",
  WATCHED: "관람완료",
};

const STATUS_BG: Record<BookingItem["status"], string> = {
  CONFIRMED: "bg-blue-100 text-[#054EFD]",
  CANCELED: "bg-red-100 text-red-600",
  PENDING_PAYMENT: "bg-yellow-100 text-yellow-700",
  WATCHED: "bg-gray-100 text-gray-700",
};

export function BookingItemCard({ booking, onCancel, onDetail }: Props) {
  const cancellable =
    booking.status === "CONFIRMED" || booking.status === "PENDING_PAYMENT";

  return (
    <Card className="space-y-4 p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-2">
          <Badge className={STATUS_BG[booking.status]}>
            {STATUS_LABEL[booking.status]}
          </Badge>
          <h3 className="text-lg font-bold">{booking.eventTitle}</h3>
          <div className="space-y-1 text-sm text-muted-foreground">
            <div className="flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5" />
              {formatDate(booking.startAt)} {formatTime(booking.startAt)}
            </div>
            <div className="flex items-center gap-1.5">
              <MapPin className="h-3.5 w-3.5" />
              {booking.venue}
            </div>
            <p className="pt-1">좌석: {booking.seatLabels.join(", ")}</p>
          </div>
        </div>
        <p className="shrink-0 text-xl font-bold text-[#1C5EFD]">
          {formatPrice(booking.amount)}
        </p>
      </div>

      <div className="flex gap-2 border-t pt-4">
        <Button
          variant="outline"
          size="lg"
          className="flex-1"
          onClick={() => onDetail?.(booking.id)}
        >
          상세보기
        </Button>
        {cancellable && (
          <Button
            variant="outline"
            size="lg"
            onClick={() => onCancel?.(booking.id)}
            className="flex-1 border-[#1C5EFD] text-[#1C5EFD] hover:bg-blue-50"
          >
            예매취소
          </Button>
        )}
      </div>
    </Card>
  );
}
