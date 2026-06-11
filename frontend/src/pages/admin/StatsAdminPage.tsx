import { AdminCard } from "@/components/admin/AdminCard";
import { AdminStatCard } from "@/components/admin/StatCard";
import { useAdminDashboard } from "@/hooks";
import { formatPriceShort } from "@/lib/format";

export default function StatsAdminPage() {
  const { data } = useAdminDashboard();
  if (!data) return null;
  return (
    <AdminCard title="통계">
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
      <p className="mt-6 text-sm text-muted-foreground">
        상세 차트는 추후 제공됩니다.
      </p>
    </AdminCard>
  );
}
