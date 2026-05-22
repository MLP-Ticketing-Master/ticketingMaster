import { Calendar, Clock, MapPin, Users } from "lucide-react";
import { Card } from "@/components/ui/card";
import { formatDateRange } from "@/lib/format";
import type { EventDetailResponse } from "@/types";

export function EventInfo({ event }: { event: EventDetailResponse }) {
  return (
    <div className="space-y-6">
      {/* 경기 정보 — 위로 */}
      <Card className="p-6">
        <h3 className="text-xl font-bold">경기 정보</h3>
        <div className="mt-4 space-y-3 text-sm">
          <InfoRow icon={MapPin} label="장소" value={event.place} />
          <InfoRow
            icon={Calendar}
            label="기간"
            value={formatDateRange(event.startDate, event.endDate)}
          />
          {event.matchDurationText && (
            <InfoRow icon={Clock} label="경기시간" value={event.matchDurationText} />
          )}
          {event.ageRating && (
            <InfoRow icon={Users} label="관람등급" value={event.ageRating} />
          )}
        </div>
      </Card>

      {/* 대회 소개 — 아래로 */}
      {event.description && (
        <Card className="p-6">
          <h3 className="text-xl font-bold">대회 소개</h3>
          <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-muted-foreground">
            {event.description}
          </p>
        </Card>
      )}
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