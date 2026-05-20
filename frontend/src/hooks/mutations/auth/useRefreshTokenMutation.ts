import { useMutation } from "@tanstack/react-query";
import * as authApi from "@/api/auth.api";
import { useAuthStore } from "@/store";

export const useRefreshTokenMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: authApi.getRefresh,

    onSuccess: ({ user, accessToken }) => {
      setAuth(user, accessToken);
    },

    onError: () => {
      useAuthStore.getState().clear();
    },
  });
};