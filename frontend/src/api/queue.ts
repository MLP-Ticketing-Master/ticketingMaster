import api from "@/lib/axios";
import type { QueueEnterResponse, QueueStatusResponse } from "@/types";

/**
 * 대기열 진입
 */
export async function enterQueue(matchId: number): Promise<QueueEnterResponse> {
  const res = await api.post<QueueEnterResponse>(`/queue/${matchId}/enter`);
  return res.data;
}

/**
 * 대기열 상태 조회
 */
export async function getQueueStatus(
  matchId: number,
  queueToken: string,
): Promise<QueueStatusResponse> {
  const res = await api.get<QueueStatusResponse>(`/queue/${matchId}/status`, {
    headers: { "Queue-Token": queueToken },
  });
  return res.data;
}
