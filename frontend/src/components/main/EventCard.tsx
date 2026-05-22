import { useNavigate } from "react-router-dom";
import { Calendar, MapPin } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { SPORT_LABEL } from "@/lib/constants";
import { formatDateRange } from "@/lib/format";
import { resolveEventImage } from "@/lib/eventImages";
import type { EventListResponse } from "@/types";

interface Props {
  event: EventListResponse;
}

export function EventCard({ event }: Props) {
  const navigate = useNavigate();

  const sportLabel =
    event.sportType in SPORT_LABEL
      ? SPORT_LABEL[event.sportType as keyof typeof SPORT_LABEL]
      : event.sportType;

  return (
    <Card
      className="group cursor-pointer overflow-hidden border-0 p-0 shadow-sm transition-shadow hover:shadow-md"
      onClick={() => navigate(`/events/${event.eventId}`)}
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-gray-100">
        {(() => {
          const src = resolveEventImage(event.thumbnailUrl);
          return src ? (
            <img
              src={src}
              alt={event.title}
              loading="lazy"
              className="h-full w-full object-cover transition-transform group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-100 to-indigo-200">
              <span className="text-4xl font-bold text-indigo-400 opacity-50">
                {event.sportType}
              </span>
            </div>
          );
        })()}
      </div>
      <div className="space-y-3 p-5">
        <Badge
          variant="secondary"
          className="bg-blue-100 font-medium text-[#FF6B47]"
        >
          {sportLabel}
        </Badge>
        <h3 className="line-clamp-2 text-base font-bold leading-snug">
          {event.title}
        </h3>
        <div className="space-y-1.5 text-sm text-muted-foreground">
          <div className="flex items-center gap-1.5">
            <Calendar className="h-3.5 w-3.5" />
            {formatDateRange(event.startDate, event.endDate)}
          </div>
          <div className="flex items-center gap-1.5">
            <MapPin className="h-3.5 w-3.5" />
            {event.place}
          </div>
        </div>
      </div>
    </Card>
  );
}
