import { Outlet } from "react-router-dom";
import { Shield } from "lucide-react";
import { AdminSidebar } from "@/components/admin/AdminSidebar";

export function AdminLayout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-[#054EFD]/15 bg-[#054EFD]/5">
        <div className="mx-auto flex max-w-7xl items-center gap-4 px-8 py-6">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[#054EFD]/10 text-[#054EFD]">
            <Shield className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">관리자 페이지</h1>
            <p className="mt-0.5 text-sm text-muted-foreground">
              E스포츠 대회 및 예매 관리
            </p>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-7xl gap-6 px-8 py-8">
        <AdminSidebar />
        <main className="flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
