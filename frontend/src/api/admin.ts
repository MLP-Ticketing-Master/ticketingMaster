import api from "@/lib/axios";

export interface AdminDashboardStats {
  totalEvents: number;
  totalBookings: number;
  totalRevenue: number;
}

/** GET /admin/dashboard */
export async function getAdminDashboard(): Promise<AdminDashboardStats> {
  const res = await api.get<AdminDashboardStats>("/admin/dashboard");
  return res.data;
}
