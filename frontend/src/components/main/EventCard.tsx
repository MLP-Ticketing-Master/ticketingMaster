import { useNavigate } from "react-router-dom";
import { Calendar, MapPin } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { GAME_LABEL } from "@/lib/constants";
import { formatDateRange } from "@/lib/format";
import type { EventSummary } from "@/types";

export function EventCard({ event }: { event: EventSummary }) {
  const navigate = useNavigate();
  const game = event.game === "ALL" ? "LOL" : event.game;

  return (
    <Card
      className="group cursor-pointer overflow-hidden border-0 p-0 shadow-sm transition-shadow hover:shadow-md"
      onClick={() => navigate(`/events/${event.id}`)}
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-gray-100">
        <img
          src={event.posterUrl}
          alt={event.title}
          loading="lazy"
          className="h-full w-full object-cover transition-transform group-hover:scale-105"
        />
      </div>
      <div className="space-y-3 p-5">
        <Badge
          variant="secondary"
          className="bg-blue-100 font-medium text-[#FF6B47]"
        >
          {GAME_LABEL[game]}
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
            {event.venue}
          </div>
        </div>
      </div>
    </Card>
  );
}
