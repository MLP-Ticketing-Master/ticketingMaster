import { useMutation } from "@tanstack/react-query";
import { AxiosError } from "axios";
import * as authApi from "@/api/auth.api";
import { useAuthStore } from "@/store";
import type { LoginRequest } from "@/types";

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
      const axiosErr = error as AxiosError<{ message?: string }>;
      const message =
        axiosErr.response?.data?.message ?? "로그인 실패";
      console.error("로그인 에러:", message);
    },
  });
};
