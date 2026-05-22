import { EventCard } from "./EventCard";
import type { EventListResponse } from "@/types";

interface Props {
  events: EventListResponse[];
  fallbackEventId: number | null;
}

export function EventGrid({ events, fallbackEventId }: Props) {
  if (events.length === 0) {
    return (
      <div className="rounded-2xl border bg-white py-20 text-center text-muted-foreground">
        진행 중인 대회가 없습니다.
      </div>
    );
  }

  return (
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      {events.map((event) => (
        <EventCard
          key={event.eventId}
          event={event}
          fallbackEventId={fallbackEventId}
        />
      ))}
    </div>
  );
}