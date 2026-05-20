import { useMutation } from "@tanstack/react-query";
import * as authApi from "@/api/auth.api";
import { useAuthStore } from "@/store";
import type { LoginRequest } from "@/types";

export const useLoginMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: async (body: LoginRequest) =>{
      return authApi.getLogin(body);
      
    },
    onSuccess: ({ user, accessToken }) => {
      console.log("LOGIN SUCCESS:", user);
      setAuth(user, accessToken);
    },

    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        "로그인 실패";

      console.error("로그인 에러:", message);
    },
  });
};