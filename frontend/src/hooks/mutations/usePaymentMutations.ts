import { useMutation, useQueryClient } from "@tanstack/react-query";
import { confirmPayment } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import type { PaymentConfirmRequest } from "@/types";

/**
 * 결제 승인 — 토스 위젯에서 받은 paymentKey 를 백엔드로 전달
 * 성공 시 Booking → CONFIRMED, Seat → SOLD
 */
export const useConfirmPaymentMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: PaymentConfirmRequest) => confirmPayment(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.bookings.me });
    },
  });
};
