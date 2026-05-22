import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";

// 백엔드에 독립적인 /matches 목록 엔드포인트가 없습니다.
// 매치 정보는 useEventDetail() 반환값의 matches 배열을 사용하세요.
// 아래는 Admin 페이지의 하위 호환을 위한 stub입니다.

export const useMatches = (eventId?: number) =>
  useQuery({
    queryKey: queryKeys.matches.list(eventId),
    queryFn: async () => [] as any[],
    enabled: false, // 실제 API 없음
  });
