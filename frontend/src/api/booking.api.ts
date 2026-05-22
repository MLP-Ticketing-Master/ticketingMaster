import api from "@/lib/axios";
import type { BookingItem, CreateBookingRequest, PageResponse } from "@/types";

// 백엔드 BookingSummaryResponse → 프론트 BookingItem 변환
function toBookingItem(raw: BookingSummaryRaw): BookingItem {
  return {
    id: raw.bookingId,
    bookingNo: raw.bookingNumber,
    eventTitle: raw.eventTitle,
    roundLabel: raw.roundLabel,
    startAt: raw.matchStartAt,
    venue: "",              // 백엔드 BookingSummaryResponse에 venue 없음
    seatLabels: raw.seatCodes,
    amount: raw.totalPrice,
    status: raw.status as BookingItem["status"],
    bookedAt: raw.createdAt,
  };
}

// 백엔드 BookingSummaryResponse 원본 형태
interface BookingSummaryRaw {
  bookingId: number;
  bookingNumber: string;
  eventTitle: string;
  matchStartAt: string;
  roundLabel: string;
  seatCodes: string[];
  seatCount: number;
  totalPrice: number;
  status: string;
  createdAt: string;
}

// 백엔드 Page<BookingSummaryResponse> 형태
interface PageRaw<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const bookingApi = {
  // GET /bookings/me?status=&page=&size= → Page<BookingSummaryResponse>
  myList: (params?: { status?: string; page?: number; size?: number }) =>
    api
      .get<PageRaw<BookingSummaryRaw>>("/bookings/me", { params })
      .then((r) => r.data.content.map(toBookingItem)),

  // POST /bookings
  create: (body: CreateBookingRequest) =>
    api.post<BookingItem>("/bookings", body).then((r) => r.data),

  // POST /bookings/{id}/cancel — cancelReason body 필요 (빈 객체 전송 가능)
  cancel: (id: number, cancelReason?: string) =>
    api
      .post(`/bookings/${id}/cancel`, { cancelReason: cancelReason ?? "" })
      .then((r) => r.data),

  // GET /bookings/{bookingId} — 단건 상세
  detail: (id: number) =>
    api.get(`/bookings/${id}`).then((r) => r.data),

  adminList: (params: { q?: string; status?: string; page?: number }) =>
    api
      .get<PageResponse<BookingItem>>("/admin/bookings", { params })
      .then((r) => r.data),
};
