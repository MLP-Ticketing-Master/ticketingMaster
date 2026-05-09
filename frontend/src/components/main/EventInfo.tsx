import { Calendar, Clock, MapPin, Users } from "lucide-react";
import { Card } from "@/components/ui/card";
import { SEAT_GRADE_COLORS } from "@/lib/constants";
import { formatDateRange, formatPrice } from "@/lib/format";
import type { EventDetail } from "@/types";

export function EventInfo({ event }: { event: EventDetail }) {
  return (
    <div className="space-y-6">
      <Card className="p-6">
        <h3 className="text-xl font-bold">경기 정보</h3>
        <div className="mt-4 space-y-3 text-sm">
          <InfoRow icon={MapPin} label="장소" value={event.venue} />
          <InfoRow
            icon={Calendar}
            label="기간"
            value={formatDateRange(event.startDate, event.endDate)}
          />
          <InfoRow
            icon={Clock}
            label="경기시간"
            value={`약 ${Math.floor(event.durationMinutes / 60)}시간 (BO5)`}
          />
          <InfoRow icon={Users} label="관람등급" value={event.ageLimit} />
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="text-xl font-bold">티켓 가격</h3>
        <ul className="mt-4 space-y-2">
          {event.prices.map((price) => (
            <li
              key={price.gradeCode}
              className={`flex items-center justify-between rounded-lg bg-${price.color}-50 px-4 py-3`}
            >
              <div className="flex items-center gap-2.5">
                <span
                  className={`h-3 w-3 rounded-sm ${SEAT_GRADE_COLORS[price.gradeCode] ?? "bg-gray-400"}`}
                />
                <span className="font-semibold">{price.gradeName}</span>
              </div>
              <span className="font-bold">{formatPrice(price.price)}</span>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}

function InfoRow({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof MapPin;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-2 text-muted-foreground">
      <Icon className="h-4 w-4" />
      <span className="w-20 text-sm">{label}</span>
      <span className="font-medium text-foreground">{value}</span>
    </div>
  );
}
