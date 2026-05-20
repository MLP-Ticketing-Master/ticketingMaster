import { useBookingFlowStore } from "@/store";
import { useSections } from "@/hooks";
import { ZoneSelector } from "../ZoneSelector";

export function ZoneStep() {
  const eventId = useBookingFlowStore((s) => s.eventId);
  const goToSeat = useBookingFlowStore((s) => s.goToSeat);
  const { data: sections = [] } = useSections(eventId ?? 0);

  return <ZoneSelector sections={sections} onSelect={goToSeat} />;
}
