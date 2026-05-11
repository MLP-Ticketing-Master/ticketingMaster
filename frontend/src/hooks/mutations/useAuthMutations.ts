import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api";
import { useAuthStore } from "@/store";
import type { LoginRequest, SignupRequest } from "@/types";
import { MOCK_USER } from "@/lib/mock";

const useMock = true;

export const useLoginMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  return useMutation({
    mutationFn: useMock
      ? async (body: LoginRequest) => ({
          accessToken: "mock-token",
          user: { ...MOCK_USER, email: body.email },
        })
      : (body: LoginRequest) => authApi.login(body),
    onSuccess: ({ user, accessToken }) => setAuth(user, accessToken),
  });
};

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
    onSuccess: ({ user, accessToken }) => setAuth(user, accessToken),
  });
};

export const useLogoutMutation = () => {
  const clear = useAuthStore((s) => s.clear);
  return useMutation({
    mutationFn: useMock ? async () => ({}) : () => authApi.logout(),
    onSuccess: () => clear(),
  });
};
