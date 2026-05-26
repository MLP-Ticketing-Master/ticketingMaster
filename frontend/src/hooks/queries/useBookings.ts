import { useQuery } from "@tanstack/react-query";
import { getAdminBookings, getBookingDetail, getMyBookings } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

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
    queryFn: () => getAdminBookings({ status, page }),
  });
