import { useMutation } from "@tanstack/react-query";
import * as authApi from "@/api/auth.api";


import { useAuthStore } from "@/store";
import type { SignupRequest } from "@/types";

export const useSignupMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: async (body: SignupRequest) =>{
      return authApi.getSignup(body);
      
    },
    
    onSuccess: ({ user, accessToken }) => {
          console.log("회원가입 성공:", user);
          setAuth(user, accessToken);

      },
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        "회원가입 실패";

      console.error("회원가입 에러:", message);
    },
  });
};