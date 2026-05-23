export type QueueStatus = "WAITING" | "ALLOWED";

export interface QueueEnterResponse {
  queueToken: string;
  status: QueueStatus;
  queueNumber: number;
  remainingAhead: number;
  estimatedWaitSeconds: number;
  enteredAt: string;
  // burst 게이트 통과 시 즉시 ALLOWED. WAITING 응답에서는 null
  allowedAt: string | null;
  entryDeadline: string | null;
}

export interface QueueStatusResponse {
  status: QueueStatus;
  queueNumber: number | null;
  remainingAhead: number | null;
  estimatedWaitSeconds: number | null;
  enteredAt: string;
  allowedAt: string | null;
  entryDeadline: string | null;
}
