import { http } from "@/lib/axios";
import type { EventSummary, Round, SeatGrade, Section } from "@/types";

export interface AdminDashboardStats {
  totalEvents: number;
  totalBookings: number;
  totalRevenue: number;
}

export const adminApi = {
  dashboard: () =>
    http
      .get<AdminDashboardStats>("/admin/dashboard")
      .then((r) => r.data),
  events: () =>
    http.get<EventSummary[]>("/admin/events").then((r) => r.data),
  rounds: () => http.get<Round[]>("/admin/rounds").then((r) => r.data),
  seatGrades: (eventId: number) =>
    http
      .get<SeatGrade[]>(`/admin/events/${eventId}/seat-grades`)
      .then((r) => r.data),
  sections: (eventId: number) =>
    http
      .get<Section[]>(`/admin/events/${eventId}/sections`)
      .then((r) => r.data),
};
