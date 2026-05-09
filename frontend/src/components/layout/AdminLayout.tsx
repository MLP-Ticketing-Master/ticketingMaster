import { Outlet } from "react-router-dom";
import { AdminSidebar } from "@/components/admin/AdminSidebar";

export function AdminLayout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-[#2D2F3E] px-8 py-7">
        <div className="mx-auto max-w-7xl">
          <h1 className="text-2xl font-bold text-white">관리자 페이지</h1>
          <p className="mt-1 text-sm text-gray-400">
            E스포츠 대회 및 예매 관리
          </p>
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
