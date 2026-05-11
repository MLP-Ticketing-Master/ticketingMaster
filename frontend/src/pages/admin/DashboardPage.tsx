import { AdminCard } from "@/components/admin/AdminCard";
import { AdminStatCard } from "@/components/admin/StatCard";
import { useAdminDashboard } from "@/hooks";
import { formatPriceShort } from "@/lib/format";

export default function DashboardPage() {
  const { data } = useAdminDashboard();
  if (!data) return null;

  return (
    <AdminCard title="대시보드">
      <div className="grid gap-4 md:grid-cols-3">
        <AdminStatCard
          label="전체 대회"
          value={String(data.totalEvents)}
          tone="rose"
        />
        <AdminStatCard
          label="총 예매 건수"
          value={data.totalBookings.toLocaleString()}
          tone="blue"
        />
        <AdminStatCard
          label="총 매출"
          value={formatPriceShort(data.totalRevenue)}
          tone="green"
        />
      </div>
    </AdminCard>
  );
}
