import { useMemo } from "react";
import { useBookingFlowStore } from "@/store";
import { useSeatSections } from "@/hooks";
import { ZoneSelector } from "../ZoneSelector";
import type { Section } from "@/types";

export function ZoneStep() {
  const matchId = useBookingFlowStore((s) => s.matchId);
  const goToSeat = useBookingFlowStore((s) => s.goToSeat);

  const { data: sectionData } = useSeatSections(matchId);

  // SeatSectionListResponse.sections → Section[] 어댑터
  const sections: Section[] = useMemo(
    () =>
      (sectionData?.sections ?? []).map((s) => ({
        id: s.sectionId,
        name: s.name,
        description: `잔여 ${s.availableCount}석`,
        sortOrder: s.displayOrder,
      })),
    [sectionData],
  );

  return <ZoneSelector sections={sections} onSelect={goToSeat} />;
}
