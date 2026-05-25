import api from "@/lib/axios";
import type {
  BookingItem,
  BookingResponse,
  CreateBookingRequest,
  PageResponse,
} from "@/types";

// 백엔드 BookingSummaryResponse 원본 형태
interface BookingSummaryRaw {
  bookingId: number;
  bookingNumber: string;
  eventTitle: string;
  place: string;
  matchStartAt: string;
  roundLabel: string;
  seatCodes: string[];
  seatCount: number;
  totalPrice: number;
  status: string;
  createdAt: string;
}

interface PageRaw<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// 백엔드 BookingSummaryResponse → 프론트 BookingItem 변환
function toBookingItem(raw: BookingSummaryRaw): BookingItem {
  return {
    id: raw.bookingId,
    bookingNo: raw.bookingNumber,
    eventTitle: raw.eventTitle,
    roundLabel: raw.roundLabel,
    startAt: raw.matchStartAt,
    venue: raw.place,
    seatLabels: raw.seatCodes,
    amount: raw.totalPrice,
    status: raw.status as BookingItem["status"],
    bookedAt: raw.createdAt,
  };
}

/** GET /bookings/me — 내 예매 목록 */
export async function getMyBookings(params?: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<BookingItem[]> {
  const res = await api.get<PageRaw<BookingSummaryRaw>>("/bookings/me", {
    params,
  });
  return res.data.content.map(toBookingItem);
}

/** POST /bookings — PENDING 상태로 예매 생성 (토스 결제 진입 직전 호출) */
export async function createBooking(
  body: CreateBookingRequest,
): Promise<BookingResponse> {
  const res = await api.post<BookingResponse>("/bookings", body);
  return res.data;
}

/** POST /bookings/{id}/cancel — 예매 취소 */
export async function cancelBooking(
  id: number,
  cancelReason?: string,
): Promise<unknown> {
  const res = await api.post(`/bookings/${id}/cancel`, {
    cancelReason: cancelReason ?? "",
  });
  return res.data;
}

/** GET /bookings/{bookingId} — 단건 상세 */
export async function getBookingDetail(id: number): Promise<BookingResponse> {
  const res = await api.get<BookingResponse>(`/bookings/${id}`);
  return res.data;
}

/** GET /admin/bookings — 어드민 예매 목록 */
export async function getAdminBookings(params: {
  q?: string;
  status?: string;
  page?: number;
}): Promise<PageResponse<BookingItem>> {
  const res = await api.get<PageResponse<BookingItem>>("/admin/bookings", {
    params,
  });
  return res.data;
}
