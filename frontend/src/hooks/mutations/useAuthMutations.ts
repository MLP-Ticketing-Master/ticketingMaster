import { useMutation } from "@tanstack/react-query";
import * as authApi from "@/api/auth";
import { useAuthStore } from "@/store";
import { resolveErrorMessage } from "@/lib/error";
import type { LoginRequest, SignupRequest } from "@/types";

export const useLoginMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: async (body: LoginRequest) => {
      return authApi.getLogin(body);
    },
    onSuccess: ({ user, accessToken, refreshToken }) => {
      setAuth(user, accessToken, refreshToken);
    },
    onError: (error) => {
      console.error("로그인 에러:", resolveErrorMessage(error, "로그인 실패"));
    },
  });
};

export const useLogoutMutation = () => {
  const clear = useAuthStore((s) => s.clear);

  return useMutation({
    mutationFn: authApi.getLogout,
    onSuccess: () => {
      clear();
    },
    onError: () => {
      clear();
    },
  });
};

export const useSignupMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: async (body: SignupRequest) => {
      return authApi.getSignup(body);
    },
    onSuccess: ({ user, accessToken, refreshToken }) => {
      setAuth(user, accessToken, refreshToken);
    },
    onError: (error) => {
      console.error(
        "회원가입 에러:",
        resolveErrorMessage(error, "회원가입 실패"),
      );
    },
  });
};
