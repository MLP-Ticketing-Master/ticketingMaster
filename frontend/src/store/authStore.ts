import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/types";
import { useBookingFlowStore } from "./bookingFlowStore";

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isLoading: boolean;
  error: string | null;

  // Actions
  setAuth: (user: User, accessToken: string, refreshToken?: string) => void;
  setTokens: (accessToken: string, refreshToken?: string) => void;
  clear: () => void;
  setLoading: (isLoading: boolean) => void;
  setError: (error: string | null) => void;

  // Selectors
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
  getToken: () => string | null;
  getRefreshToken: () => string | null;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isLoading: false,
      error: null,

      /**
       * 인증 정보 설정 — 로그인/회원가입 직후 호출
       * refreshToken 옵셔널 (호출자가 응답에 포함된 경우만 전달)
       */
      setAuth: (user, accessToken, refreshToken) => {
        // 계정 전환 시 이전 사용자의 예매/대기열 흐름 잔재 제거
        useBookingFlowStore.getState().reset();
        localStorage.setItem("accessToken", accessToken);
        set((prev) => ({
          user,
          accessToken,
          refreshToken: refreshToken ?? prev.refreshToken,
          error: null,
        }));
      },

      /**
       * 토큰만 갱신 (refresh 응답 처리용) — user 정보는 유지
       */
      setTokens: (accessToken, refreshToken) => {
        localStorage.setItem("accessToken", accessToken);
        set((prev) => ({
          accessToken,
          refreshToken: refreshToken ?? prev.refreshToken,
        }));
      },

      /**
       * 인증 정보 삭제 — 로그아웃 / 갱신 실패 시 호출
       */
      clear: () => {
        // 로그아웃 시 예매/대기열 흐름도 초기화 — 다음 사용자에게 이어지지 않도록
        useBookingFlowStore.getState().reset();
        localStorage.removeItem("accessToken");
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          error: null,
        });
      },

      setLoading: (isLoading) => set({ isLoading }),
      setError: (error) => set({ error }),

      isAuthenticated: () => !!get().accessToken,
      isAdmin: () => get().user?.role === "ADMIN",
      getToken: () => get().accessToken,
      getRefreshToken: () => get().refreshToken,
    }),
    {
      name: "auth-storage",
      // localStorage에 저장할 필드 정의
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    },
  ),
);

/**
 * 인증 상태를 선택적으로 구독하는 훅
 * - 불필요한 리렌더링 방지
 */
export const useAuthUser = () => useAuthStore((state) => state.user);
export const useAuthToken = () => useAuthStore((state) => state.accessToken);
export const useIsAuthenticated = () =>
  useAuthStore((state) => state.isAuthenticated());
export const useIsAdmin = () => useAuthStore((state) => state.isAdmin());
