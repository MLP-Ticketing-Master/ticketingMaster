import { Link, NavLink, useNavigate } from "react-router-dom";
import { User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store";
import { cn } from "@/lib/utils";
import logo from "@/image/logo1.jpg";
import logomovie from "@/image/LogoMovie.gif";
import { useState } from "react";

const NAV = [
  { to: "/", label: "홈", end: true },
  { to: "/events", label: "대회 일정" },
  { to: "/events?game=LOL", label: "리그 오브 레전드" },
  { to: "/events?game=VALORANT", label: "발로란트" },
  { to: "/events?game=OVERWATCH", label: "오버워치" },
  //{ to: "/admin", label: "관리자" },
];

export function Header() {
  const user = useAuthStore((s) => s.user);
  const isAuth = useAuthStore((s) => s.isAuthenticated)();
  const navigate = useNavigate();
  const [isHovering, setIsHovering] = useState(false);
  return (
    <header className="sticky top-0 z-40 border-b bg-white">
      <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-6">
        <div className="flex items-center gap-5">
          <Link to="/" className="flex items-center gap-2">
            <img
              src={isHovering ? logomovie : logo}
              alt="티켓팅마스터"
              className="h-23 w-auto scale-100 transition-all duration-300 cursor-pointer -translate-y-4"
              onMouseEnter={() => setIsHovering(true)}
              onMouseLeave={() => setIsHovering(false)}
            />
          </Link>
          <nav className="hidden items-center gap-10 md:flex">
            {NAV.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    "transition-colors hover:text-[#1C5EFD]",
                    isActive ? "text-foreground" : "text-muted-foreground",
                  )
                }
              > 
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {isAuth && (
            <button
              type="button"
              onClick={() => navigate("/my")}
              className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
            >
              <User className="h-4 w-4" />
              마이페이지
            </button>
          )}
          {isAuth ? (
            <span className="text-sm font-medium">{user?.nickname}님</span>
          ) : (
            <Button
              size="sm"
              onClick={() => navigate("/login")}
              className="bg-[#054EFD] hover:bg-[#1C5EFD]"
            >
              로그인
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
