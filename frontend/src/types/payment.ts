export type PaymentMethod = "CARD" | "EASY_PAY" | "BANK_TRANSFER";
export type PaymentStatus = "SUCCESS" | "FAILED" | "CANCELED";

// POST /payments/confirm 요청
export interface PaymentConfirmRequest {
  bookingId: number;
  paymentKey: string;
  orderId: string;
  amount: number;
}

// POST /payments/confirm 응답
export interface PaymentResponse {
  paymentId: number;
  bookingId: number;
  method: PaymentMethod;
  amount: number;
  status: PaymentStatus;
  paidAt: string | null;
  failureReason: string | null;
}
