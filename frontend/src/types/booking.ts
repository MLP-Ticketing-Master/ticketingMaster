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

export interface CreateBookingRequest {
  matchId: number;
  seatIds: number[];
}
