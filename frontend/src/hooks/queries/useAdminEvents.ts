import { useQuery } from "@tanstack/react-query";
import { getAdminEventDetail, getAdminEvents } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

export const useAdminEvents = () =>
  useQuery({
    queryKey: queryKeys.admin.events,
    queryFn: () => getAdminEvents({ page: 0, size: 50 }),
  });

export const useAdminEventDetail = (eventId: number | null) =>
  useQuery({
    queryKey: queryKeys.admin.eventDetail(eventId ?? 0),
    queryFn: () => getAdminEventDetail(eventId!),
    enabled: eventId !== null,
  });
