import api from "@/lib/axios";
import type { EventSummary, Match, SeatGradeCode } from "@/types";

export interface AdminDashboardStats {
  totalEvents: number;
  totalBookings: number;
  totalRevenue: number;
}

// 어드민 전용 좌석 등급/구역 DTO (백엔드 admin API 정합화 전까지 사용)
export interface AdminSeatGrade {
  code: SeatGradeCode;
  name: string;
  price: number;
  color: string;
  sortOrder: number;
}

export interface AdminSection {
  id: number;
  name: string;
  description: string;
  sortOrder: number;
}

export const adminApi = {
  dashboard: () =>
    api.get<AdminDashboardStats>("/admin/dashboard").then((r) => r.data),
  events: () =>
    api.get<EventSummary[]>("/admin/events").then((r) => r.data),
  matches: () => api.get<Match[]>("/admin/matches").then((r) => r.data),
  seatGrades: (eventId: number) =>
    api
      .get<AdminSeatGrade[]>(`/admin/events/${eventId}/seat-grades`)
      .then((r) => r.data),
  sections: (eventId: number) =>
    api
      .get<AdminSection[]>(`/admin/events/${eventId}/sections`)
      .then((r) => r.data),
};
