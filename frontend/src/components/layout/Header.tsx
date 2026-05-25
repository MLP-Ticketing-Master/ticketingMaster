import { Link, NavLink, useNavigate } from "react-router-dom";
import { ChevronDown, LogOut, Ticket, User as UserIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuthStore } from "@/store";
import { useLogoutMutation } from "@/hooks";
import { cn } from "@/lib/utils";
import logo from "@/assets/logo1.jpg";
import logomovie from "@/assets/LogoMovie.gif";
import { useState } from "react";

const NAV = [
  { to: "/", label: "홈", end: true },
  { to: "/events", label: "대회 일정" },
  { to: "/promotions", label: "이벤트" },
];

export function Header() {
  const user = useAuthStore((s) => s.user);
  const isAuth = useAuthStore((s) => s.isAuthenticated)();
  const navigate = useNavigate();
  const logout = useLogoutMutation();
  const [isHovering, setIsHovering] = useState(false);

  const handleLogout = () => {
    logout.mutate(undefined, {
      onSuccess: () => {
        toast.success("로그아웃되었습니다.");
        navigate("/");
      },
      onError: () => {
        toast.error("로그아웃에 실패했습니다.");
        navigate("/");
      },
    });
  };

  return (
    <header className="sticky top-0 z-40 border-b bg-white">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-3 sm:h-20 sm:px-6">
        <div className="flex items-center gap-2 sm:gap-5">
          <Link to="/" className="flex items-center">
            <img
              src={isHovering ? logomovie : logo}
              alt="티켓팅마스터"
              className="h-12 w-auto -translate-y-2 cursor-pointer transition-all duration-300 sm:h-23 sm:-translate-y-4"
              onMouseEnter={() => setIsHovering(true)}
              onMouseLeave={() => setIsHovering(false)}
            />
          </Link>
          <nav className="flex items-center gap-3 text-xs sm:gap-10 sm:text-base">
            {NAV.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    "whitespace-nowrap transition-colors hover:text-[#1C5EFD]",
                    isActive ? "text-foreground" : "text-muted-foreground",
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-1 sm:gap-2">
          {isAuth ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  className="flex items-center gap-1.5 rounded-md px-1.5 py-1 text-xs font-medium hover:bg-gray-50 focus:outline-none sm:gap-2 sm:px-2 sm:py-1.5 sm:text-sm"
                >
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-sky-100 sm:h-7 sm:w-7">
                    <Ticket className="h-3.5 w-3.5 -rotate-45 text-[#054EFD] sm:h-4 sm:w-4" />
                  </span>
                  <span>{user?.nickname}님</span>
                  <ChevronDown className="h-3.5 w-3.5 text-muted-foreground sm:h-4 sm:w-4" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-40">
                <DropdownMenuItem onSelect={() => navigate("/my")}>
                  <UserIcon className="h-4 w-4" />
                  마이페이지
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  variant="destructive"
                  onSelect={handleLogout}
                >
                  <LogOut className="h-4 w-4" />
                  로그아웃
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => navigate("/login")}
                className="h-7 px-2 text-xs sm:h-9 sm:px-3 sm:text-sm"
              >
                로그인
              </Button>
              <Button
                size="sm"
                onClick={() => navigate("/signup")}
                className="h-7 bg-[#054EFD] px-2 text-xs hover:bg-[#1C5EFD] sm:h-9 sm:px-3 sm:text-sm"
              >
                회원가입
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
