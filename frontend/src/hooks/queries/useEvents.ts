import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_EVENTS } from "@/lib/mock";
import type { SportType, EventListResponse } from "@/types";

// mock 이벤트 eventId 범위 (1~999)
// 실제 DB ID와 겹치지 않도록 mock.ts의 eventId를 1001~로 설정했으므로
// 1000 이상이면 mock으로 판별
export const isMockEvent = (eventId: number) => eventId >= 1000;

export interface EventListResult {
  events: EventListResponse[];
  /** mock 이벤트 클릭 시 대신 보여줄 실제 이벤트 ID. 실제 이벤트 없으면 null */
  fallbackEventId: number | null;
}

export const useEventList = (sportType: SportType = "ALL", page = 0, size = 20) =>
  useQuery({
    queryKey: queryKeys.events.list(sportType),
    queryFn: async (): Promise<EventListResult> => {
      let realEvents: EventListResponse[] = [];
      try {
        const pageData = await eventApi.list({
          ...(sportType !== "ALL" ? { sportType } : {}),
          page,
          size,
        });
        realEvents = pageData.content;
      } catch {
        // API 서버 미실행 시 mock만 표시
      }

      const mocks = MOCK_EVENTS.filter(
        (m) => sportType === "ALL" || m.sportType === sportType,
      );

      const realIds = new Set(realEvents.map((e) => e.eventId));
      const dedupedMocks = mocks.filter((m) => !realIds.has(m.eventId));

      return {
        events: [...realEvents, ...dedupedMocks],
        fallbackEventId: realEvents[0]?.eventId ?? null,
      };
    },
  });

export const useEventDetail = (id: number) =>
  useQuery({
    queryKey: queryKeys.events.detail(id),
    queryFn: () => eventApi.detail(id),
    enabled: !!id,
  });