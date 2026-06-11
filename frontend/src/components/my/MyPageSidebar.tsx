import { NavLink, useNavigate } from "react-router-dom";
import { User, Receipt, Settings, Lock, LogOut } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { useLogoutMutation } from "@/hooks";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

const NAV: NavItem[] = [
  { to: "/my", label: "내 정보 조회", icon: User, end: true },
  { to: "/my/bookings", label: "예매 내역", icon: Receipt },
  { to: "/my/profile", label: "회원정보 수정", icon: Settings },
  { to: "/my/password", label: "비밀번호 변경", icon: Lock },
];

export function MyPageSidebar() {
  const navigate = useNavigate();
  const logout = useLogoutMutation();

  const handleLogout = () => {
    logout.mutate(undefined, { onSuccess: () => navigate("/") });
  };

  return (
    <aside className="w-full lg:w-64 shrink-0">
      <nav className="rounded-2xl border bg-white p-2 lg:p-4 shadow-sm">
        <ul className="flex gap-2 overflow-x-auto lg:block lg:space-y-1 scrollbar-hide">
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={end}
                className={({ isActive }) =>
                  cn(
                    `flex items-center gap-2
                      whitespace-nowrap
                      rounded-lg
                      px-3 py-2
                      lg:px-4 lg:py-3
                      text-xs lg:text-sm
                      font-medium
                      transition-colors
                      shrink-0`,
                    isActive
                      ? "bg-[#1C5EFD] text-white"
                      : "text-gray-700 hover:bg-gray-100",
                  )
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            </li>
          ))}
          <li>
            <button
              type="button"
              onClick={handleLogout}
              className="flex items-center gap-2
              whitespace-nowrap
              rounded-lg
              px-3 py-2
              lg:px-4 lg:py-3
              text-xs lg:text-sm
              font-medium
              text-red-500
              transition-colors
              hover:bg-red-50
              shrink-0"
            >
              <LogOut className="h-4 w-4" />
              로그아웃
            </button>
          </li>
        </ul>
      </nav>
    </aside>
  );
}
