import { useQuery } from "@tanstack/react-query";
import { getAdminBookings, getBookingDetail, getMyBookings } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_BOOKINGS } from "@/lib/mock";

// 환경변수 기반 목 분기 — 어드민 목록은 개발 중 목 유지
const useMock = import.meta.env.VITE_USE_MOCK === "true";

export const useMyBookings = (params?: {
  status?: string;
  page?: number;
  size?: number;
}) =>
  useQuery({
    queryKey: [...queryKeys.bookings.me, params?.status, params?.page],
    queryFn: () => getMyBookings(params),
    staleTime: 1000 * 30,
  });

export const useBookingDetail = (bookingId: number | null) =>
  useQuery({
    queryKey: queryKeys.bookings.detail(bookingId ?? 0),
    queryFn: () => getBookingDetail(bookingId!),
    enabled: bookingId !== null,
  });

interface AdminBookingsParams {
  q?: string;
  status?: string;
  page?: number;
}

export const useAdminBookings = ({ q, status, page = 0 }: AdminBookingsParams) =>
  useQuery({
    queryKey: queryKeys.bookings.admin(q, status, page),
    queryFn: useMock
      ? async () => ({
          content: MOCK_BOOKINGS.filter((b) => {
            if (q && !b.bookingNo.includes(q) && !b.customerName?.includes(q))
              return false;
            if (status && status !== "ALL" && b.status !== status) return false;
            return true;
          }),
          totalElements: MOCK_BOOKINGS.length,
          totalPages: 2,
          number: page,
          size: 10,
        })
      : () => getAdminBookings({ q, status, page }),
  });
