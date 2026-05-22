import { useMutation } from "@tanstack/react-query";
import * as authApi from "@/api/auth.api";
import { useAuthStore } from "@/store";

export const useRefreshTokenMutation = () => {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: authApi.getRefresh,

    // refresh 응답엔 user 정보 없음 → 스토어의 기존 user 유지하면서 토큰만 갱신
    onSuccess: ({ accessToken }) => {
      const { user } = useAuthStore.getState();
      if (user) {
        setAuth(user, accessToken);
      } else {
        useAuthStore.getState().clear();
      }
    },

    onError: () => {
      useAuthStore.getState().clear();
    },
  });
};
