import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api";
import { useAuthStore } from "@/store";
import type { LoginRequest, SignupRequest } from "@/types";
import { MOCK_USER } from "@/lib/mock";
import { http } from "@/lib/axios";

const useMock = false; // 프로덕션에서는 false로 설정
 
/**
 * 로그인 뮤테이션
 * - API 호출 또는 Mock 데이터 사용
 * - 성공 시 accessToken과 user를 Zustand에 저장
 * - 토큰은 localStorage에 자동 저장
 */

export const useLoginMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: useMock
      ? async (body: LoginRequest) => ({
          accessToken: "mock-token",
          user: { ...MOCK_USER, email: body.email },
        })
      : (body: LoginRequest) => authApi.login(body),
    onSuccess: ({ user, accessToken }) => {setAuth(user, accessToken)},

    onError: (error: any) => {
      const message = error.response?.data?.message || "로그인 실패";
      throw new Error(message);
    },
  });
};
/**
 * 회원가입 뮤테이션
 * - 회원가입 후 자동 로그인
 * - accessToken과 user를 저장
 */

export const useSignupMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  return useMutation({
    mutationFn: useMock
      ? async (body: SignupRequest) => ({
          accessToken: "mock-token",
          user: {
            ...MOCK_USER,
            name: body.name,
            email: body.email,
            phone: body.phone,
          },
        })
      : (body: SignupRequest) => authApi.signup(body),
    onSuccess: ({ user, accessToken }) => {setAuth(user, accessToken)},

    onError: (error: any) => {
      const message = error.response?.data?.message || "회원가입 실패";
      throw new Error(message);
    },
  });
};

/**
 * 로그아웃 뮤테이션
 * - 서버에 로그아웃 요청
 * - 모든 로컬 데이터 삭제
 */
export const useLogoutMutation = () => {
  const clear = useAuthStore((s) => s.clear);
  return useMutation({
    mutationFn: useMock ? async () => ({}) : () => authApi.logout(),
    onSuccess: () => {clear()},
    onError: () => {
      // 로그아웃 실패해도 로컬 데이터는 삭제
      clear();
    },
  });
};

/**
 * 토큰 갱신 뮤테이션
 * - accessToken 만료 시 새 토큰 획득
 * - 자동으로 axios 인터셉터에서 호출됨
 */
export const useRefreshTokenMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  
  return useMutation({
    mutationFn: async () => {
      const response = await http.post<{
        accessToken: string;
        user: any;
      }>("/auth/refresh");
      return response.data;
    },
    onSuccess: ({ accessToken, user }) => {
      setAuth(user, accessToken);
    },
    onError: () => {
      // 갱신 실패 시 로그아웃
      useAuthStore.getState().clear();
    },
  });
};
 
/**
 * 현재 사용자 정보 조회
 * - 앱 초기화 시 호출하여 인증 상태 확인
 */
export const useMeQuery = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  const accessToken = useAuthStore((s) => s.accessToken);
  
  return {
    async fetchMe() {
      if (!accessToken) return null;
      
      try {
        const user = await authApi.me();
        return user;
      } catch (error) {
        // 토큰이 유효하지 않으면 로그아웃
        useAuthStore.getState().clear();
        return null;
      }
    },
  };
};