import api from "@/lib/axios";
import type { BookingItem, CreateBookingRequest, PageResponse } from "@/types";

export const bookingApi = {
  myList: () =>
    api.get<BookingItem[]>("/me/bookings").then((r) => r.data),
  create: (body: CreateBookingRequest) =>
    api.post<BookingItem>("/bookings", body).then((r) => r.data),
  cancel: (id: number) =>
    api.post(`/bookings/${id}/cancel`).then((r) => r.data),
  adminList: (params: { q?: string; status?: string; page?: number }) =>
    api
      .get<PageResponse<BookingItem>>("/admin/bookings", { params })
      .then((r) => r.data),
};
