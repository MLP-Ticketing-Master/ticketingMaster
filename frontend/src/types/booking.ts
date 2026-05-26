import type { BookingStatus } from "./common";

export interface BookingItem {
  id: number;
  bookingNo: string;
  eventTitle: string;
  roundLabel: string;
  startAt: string;
  venue: string;
  seatLabels: string[];
  amount: number;
  status: BookingStatus;
  bookedAt: string;
  customerName?: string;
  customerEmail?: string;
  paymentMethod?: string;
}

/** GET /admin/bookings 응답 요소 (백엔드 AdminBookingListResponse) */
export interface AdminBookingListResponse {
  bookingId: number;
  bookingNumber: string;
  userId: number;
  userNickname: string;
  userEmail: string;
  eventTitle: string;
  matchStartAt: string;
  roundLabel: string | null;
  seatCodes: string[];
  seatCount: number;
  totalPrice: number;
  status: BookingStatus;
  createdAt: string;
}

export interface CreateBookingRequest {
  matchId: number;
  seatIds: number[];
}

// 백엔드 BookingResponse 매핑 — POST /bookings, GET /bookings/{id} 응답
export interface BookingResponse {
  bookingId: number;
  bookingNumber: string;
  matchInfo: {
    matchId: number;
    eventTitle: string;
    roundLabel: string | null;
    homeTeamName: string | null;
    awayTeamName: string | null;
    startAt: string;
  };
  seats: Array<{
    seatId: number;
    seatCode: string;
    gradeCode: string;
    seatPrice: number;
  }>;
  totalPrice: number;
  status: BookingStatus;
  createdAt: string;
}
