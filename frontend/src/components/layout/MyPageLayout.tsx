import { Outlet } from "react-router-dom";
import { Header } from "./Header";
import { Footer } from "./Footer";
import { MyPageSidebar } from "@/components/my/MyPageSidebar";

export function MyPageLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <Header />
      <main className="flex-1">
        <div className="mx-auto max-w-7xl px-6 py-10">
          <h1 className="mb-6 text-3xl font-bold">마이페이지</h1>
          <div className="flex gap-6">
            <MyPageSidebar />
            <div className="flex-1">
              <Outlet />
            </div>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
