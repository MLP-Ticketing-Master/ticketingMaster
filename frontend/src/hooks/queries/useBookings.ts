import { useQuery } from "@tanstack/react-query";
import { bookingApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { MOCK_BOOKINGS } from "@/lib/mock";

const useMock = true;

export const useMyBookings = () =>
  useQuery({
    queryKey: queryKeys.bookings.me,
    queryFn: useMock ? async () => MOCK_BOOKINGS : () => bookingApi.myList(),
  });

interface AdminBookingsParams {
  q?: string;
  status?: string;
  page?: number;
}

export const useAdminBookings = ({
  q,
  status,
  page = 0,
}: AdminBookingsParams) =>
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
      : () => bookingApi.adminList({ q, status, page }),
  });
