import { useQuery } from "@tanstack/react-query";
import { seatApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import {
  MOCK_SEAT_GRADES,
  MOCK_SEAT_LAYOUT,
  MOCK_SECTIONS,
} from "@/lib/mock";

const useMock = true;

export const useSeatGrades = (eventId: number) =>
  useQuery({
    queryKey: ["seat-grades", eventId],
    queryFn: useMock
      ? async () => MOCK_SEAT_GRADES
      : () => seatApi.grades(eventId),
    enabled: !!eventId,
  });

export const useSections = (eventId: number) =>
  useQuery({
    queryKey: ["sections", eventId],
    queryFn: useMock
      ? async () => MOCK_SECTIONS
      : () => seatApi.sections(eventId),
    enabled: !!eventId,
  });

export const useSeatLayout = (matchId: number, sectionId?: number) =>
  useQuery({
    queryKey: queryKeys.matches.seatLayout(matchId),
    queryFn: useMock
      ? async () => MOCK_SEAT_LAYOUT
      : () => seatApi.layout(matchId, sectionId),
    enabled: !!matchId && !!sectionId,
  });
