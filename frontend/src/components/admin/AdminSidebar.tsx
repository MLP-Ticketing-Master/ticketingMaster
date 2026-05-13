import { NavLink } from "react-router-dom";
import {
  Calendar,
  Clock,
  Users,
  Armchair,
  Receipt,
  BarChart3,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

const NAV: NavItem[] = [
  { to: "/admin/events", label: "대회 관리", icon: Calendar },
  { to: "/admin/rounds", label: "회차 관리", icon: Clock },
  { to: "/admin/teams", label: "팀 관리", icon: Users },
  { to: "/admin/seats", label: "좌석 관리", icon: Armchair },
  { to: "/admin/bookings", label: "예매 관리", icon: Receipt },
  { to: "/admin/stats", label: "통계", icon: BarChart3 },
];

export function AdminSidebar() {
  return (
    <aside className="w-64 shrink-0">
      <nav className="rounded-2xl border bg-white p-4 shadow-sm">
        <ul className="space-y-1">
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={end}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-[#3C76FE] text-white"
                      : "text-gray-700 hover:bg-gray-100",
                  )
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  );
}
