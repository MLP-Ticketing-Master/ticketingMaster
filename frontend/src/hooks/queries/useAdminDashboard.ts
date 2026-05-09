import { useQuery } from "@tanstack/react-query";
import { adminApi } from "@/api";
import { queryKeys } from "@/lib/queryKeys";

const useMock = true;

export const useAdminDashboard = () =>
  useQuery({
    queryKey: queryKeys.admin.dashboard,
    queryFn: useMock
      ? async () => ({
          totalEvents: 24,
          totalBookings: 1247,
          totalRevenue: 127_000_000,
        })
      : () => adminApi.dashboard(),
  });
