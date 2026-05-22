import { useMutation } from "@tanstack/react-query";
import * as authApi from "@/api/auth.api";
import { useAuthStore } from "@/store";

export const useRefreshTokenMutation = () => {
  const setTokens = useAuthStore((s) => s.setTokens);

  return useMutation({
    mutationFn: () => {
      const refreshToken = useAuthStore.getState().refreshToken;
      if (!refreshToken) {
        return Promise.reject(new Error("Refresh Token이 없습니다."));
      }
      return authApi.getRefresh(refreshToken);
    },
    // refresh 응답엔 user 정보 없음 → 스토어 user 유지, 토큰만 갱신
    onSuccess: ({ accessToken, refreshToken }) => {
      setTokens(accessToken, refreshToken);
    },
    onError: () => {
      useAuthStore.getState().clear();
    },
  });
};
