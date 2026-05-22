import api from "@/lib/axios";
import type { PaymentConfirmRequest, PaymentResponse } from "@/types";

/**
 * POST /payments/confirm — 토스 위젯에서 받은 paymentKey 로 결제 승인
 * 성공 시 Booking → CONFIRMED, Seat → SOLD
 */
export async function confirmPayment(
  body: PaymentConfirmRequest,
): Promise<PaymentResponse> {
  const res = await api.post<PaymentResponse>("/payments/confirm", body);
  return res.data;
}
