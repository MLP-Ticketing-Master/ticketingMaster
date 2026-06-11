import { useBookingFlowStore } from "@/store";
import { useSeatSections } from "@/hooks";
import { ZoneSelector } from "../ZoneSelector";

export function ZoneStep() {
  const matchId = useBookingFlowStore((s) => s.matchId);
  const goToSeat = useBookingFlowStore((s) => s.goToSeat);
  const { data } = useSeatSections(matchId);
  const sections = data?.sections ?? [];

  return <ZoneSelector sections={sections} onSelect={goToSeat} />;
}
