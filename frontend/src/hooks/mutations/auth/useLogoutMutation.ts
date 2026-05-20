import { useMutation } from "@tanstack/react-query";
import * as authApi from "@/api/auth.api";

import { useAuthStore } from "@/store";

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