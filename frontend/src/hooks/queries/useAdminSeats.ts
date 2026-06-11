import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { getAdminSeatGrades, getAdminSections, getAdminSeats } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

export const useAdminSeatGrades = (eventId: number | null) =>
  useQuery({
    queryKey: queryKeys.admin.seatGrades(eventId ?? 0),
    queryFn: () => getAdminSeatGrades(eventId!),
    enabled: eventId !== null,
  });

export const useAdminSections = (eventId: number | null) =>
  useQuery({
    queryKey: queryKeys.admin.sections(eventId ?? 0),
    queryFn: () => getAdminSections(eventId!),
    enabled: eventId !== null,
  });

export const useAdminSeats = (matchId: number | null) =>
  useQuery({
    queryKey: queryKeys.admin.seats(matchId ?? 0),
    queryFn: () => getAdminSeats(matchId!),
    enabled: matchId !== null,
    // 회차 변경 시 이전 좌석 데이터 유지 — 페이지 높이 축소로 인한 스크롤 점프 방지
    placeholderData: keepPreviousData,
  });
