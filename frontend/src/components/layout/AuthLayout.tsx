import { Outlet } from "react-router-dom";

export function AuthLayout() {
  return (
    <div className="flex min-h-screen items-start justify-center bg-gray-50 px-4 pb-12 pt-6">
      <Outlet />
    </div>
  );
}
