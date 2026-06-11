import { Outlet } from "react-router-dom";
import { Header } from "./Header";
import { Footer } from "./Footer";
import { MyPageSidebar } from "@/components/my/MyPageSidebar";

export function MyPageLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <Header />
      <main className="flex-1">
        <div className="mx-auto max-w-7xl px-3 sm:px-6 py-6 sm:py-10">
          <h1 className="mb-4 sm:mb-6 text-2xl sm:text-3xl font-bold">
            마이페이지
          </h1>

          <div className="flex flex-col lg:flex-row gap-4 lg:gap-6">
            <MyPageSidebar />

            <div className="flex-1 min-w-0">
              <Outlet />
            </div>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
