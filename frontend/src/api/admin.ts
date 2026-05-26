import api from "@/lib/axios";
import type { Match, SeatGradeCode } from "@/types";

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

/** GET /admin/dashboard */
export async function getAdminDashboard(): Promise<AdminDashboardStats> {
  const res = await api.get<AdminDashboardStats>("/admin/dashboard");
  return res.data;
}

/** GET /admin/matches */
export async function getAdminMatches(): Promise<Match[]> {
  const res = await api.get<Match[]>("/admin/matches");
  return res.data;
}

/** GET /admin/events/{eventId}/seat-grades */
export async function getAdminSeatGrades(
  eventId: number,
): Promise<AdminSeatGrade[]> {
  const res = await api.get<AdminSeatGrade[]>(
    `/admin/events/${eventId}/seat-grades`,
  );
  return res.data;
}

/** GET /admin/events/{eventId}/sections */
export async function getAdminSections(
  eventId: number,
): Promise<AdminSection[]> {
  const res = await api.get<AdminSection[]>(
    `/admin/events/${eventId}/sections`,
  );
  return res.data;
}
