import { http } from "@/lib/axios";
import type { BookingItem, CreateBookingRequest, PageResponse } from "@/types";

export const bookingApi = {
  myList: () =>
    http.get<BookingItem[]>("/me/bookings").then((r) => r.data),
  create: (body: CreateBookingRequest) =>
    http.post<BookingItem>("/bookings", body).then((r) => r.data),
  cancel: (id: number) =>
    http.post(`/bookings/${id}/cancel`).then((r) => r.data),
  adminList: (params: { q?: string; status?: string; page?: number }) =>
    http
      .get<PageResponse<BookingItem>>("/admin/bookings", { params })
      .then((r) => r.data),
};
