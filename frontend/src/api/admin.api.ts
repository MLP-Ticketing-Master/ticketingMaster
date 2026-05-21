import api from "@/lib/axios";
import type { EventSummary, Match, SeatGrade, Section } from "@/types";

export interface AdminDashboardStats {
  totalEvents: number;
  totalBookings: number;
  totalRevenue: number;
}

export const adminApi = {
  dashboard: () =>
    api
      .get<AdminDashboardStats>("/admin/dashboard")
      .then((r) => r.data),
  events: () =>
    api.get<EventSummary[]>("/admin/events").then((r) => r.data),
  matches: () => api.get<Match[]>("/admin/matches").then((r) => r.data),
  seatGrades: (eventId: number) =>
    api
      .get<SeatGrade[]>(`/admin/events/${eventId}/seat-grades`)
      .then((r) => r.data),
  sections: (eventId: number) =>
    api
      .get<Section[]>(`/admin/events/${eventId}/sections`)
      .then((r) => r.data),
};
