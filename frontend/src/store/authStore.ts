import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/types";

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isLoading: boolean;
  error: string | null;

  // Actions
  setAuth: (user: User, accessToken: string) => void;
  clear: () => void;
  setLoading: (isLoading: boolean) => void;
  setError: (error: string | null) => void;
  
  // Selectors
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
  getToken: () => string | null;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      isLoading: false,
      error: null,
 
      /**
       * 인증 정보 설정
       * - user와 accessToken을 저장
       * - localStorage에 자동 저장 (persist 미들웨어)
       */
      setAuth: (user, accessToken) => {
        localStorage.setItem("accessToken", accessToken);
        set({ user, accessToken, error: null });
      },
 
      /**
       * 인증 정보 삭제
       * - 로그아웃 또는 토큰 갱신 실패 시 호출
       */
      clear: () => {
        localStorage.removeItem("accessToken");
        set({ user: null, accessToken: null, error: null });
      },
 
      /**
       * 로딩 상태 설정
       */
      setLoading: (isLoading) => {
        set({ isLoading });
      },
 
      /**
       * 에러 메시지 설정
       */
      setError: (error) => {
        set({ error });
      },
 
      /**
       * 인증 여부 확인
       */
      isAuthenticated: () => {
        const token = get().accessToken;
        return !!token;
      },
 
      /**
       * 관리자 여부 확인
       */
      isAdmin: () => {
        return get().user?.role === "ADMIN";
      },
 
      /**
       * 현재 토큰 반환
       */
      getToken: () => {
        return get().accessToken;
      },
    }),
    {
      name: "auth-storage",
      // localStorage에 저장할 필드 정의
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
      }),
    }
  )
);
 
/**
 * 인증 상태를 선택적으로 구독하는 훅
 * - 불필요한 리렌더링 방지
 */
export const useAuthUser = () => {
  return useAuthStore((state) => state.user);
};
 
export const useAuthToken = () => {
  return useAuthStore((state) => state.accessToken);
};
 
export const useIsAuthenticated = () => {
  return useAuthStore((state) => state.isAuthenticated());
};
 
export const useIsAdmin = () => {
  return useAuthStore((state) => state.isAdmin());
};
