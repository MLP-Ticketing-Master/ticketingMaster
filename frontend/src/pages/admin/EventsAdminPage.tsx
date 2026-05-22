import { AdminCard } from "@/components/admin/AdminCard";

export default function EventsAdminPage() {
  return (
    <div className="space-y-6">
      <AdminCard title="대회 관리">
        <div className="py-12 text-center text-sm text-muted-foreground">
          대회 관리 페이지는 어드민 전용 백엔드 API 정합화 이후 제공될 예정입니다.
        </div>
      </AdminCard>
    </div>
  );
}
