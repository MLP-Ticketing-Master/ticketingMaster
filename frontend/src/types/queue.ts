export type QueueStatus = "WAITING" | "ALLOWED";

export interface QueueEnterResponse {
  queueToken: string;
  status: QueueStatus;
  queueNumber: number;
  remainingAhead: number;
  estimatedWaitSeconds: number;
  enteredAt: string;
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
