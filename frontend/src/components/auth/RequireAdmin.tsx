import { useEffect } from "react";
import { Navigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuthStore } from "@/store";

interface Props {
  children: React.ReactNode;
}

/**
 * 관리자 전용 라우트 가드
 * - 비로그인 → /login (로그인 안내)
 * - 일반 유저 → / (권한 없음 안내)
 * - 관리자 → children 렌더
 */
export function RequireAdmin({ children }: Props) {
  const isAuth = useAuthStore((s) => s.isAuthenticated)();
  const isAdmin = useAuthStore((s) => s.isAdmin)();

  useEffect(() => {
    if (!isAuth) {
      toast.error("로그인이 필요합니다.");
    } else if (!isAdmin) {
      toast.error("관리자 권한이 필요합니다.");
    }
  }, [isAuth, isAdmin]);

  if (!isAuth) return <Navigate to="/login" replace />;
  if (!isAdmin) return <Navigate to="/" replace />;
  return <>{children}</>;
}
