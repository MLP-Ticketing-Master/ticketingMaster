import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import {
  meApi,
  type ChangePasswordRequest,
  type UpdateProfileRequest,
} from "@/api";
import { queryKeys } from "@/lib/queryKeys";
import { useAuthStore } from "@/store";

/**
 * 프로필 업데이트 뮤테이션
 * PATCH /users/me — nickname, phone만 수정 (email 변경 불가)
 */
export const useUpdateProfileMutation = () => {
  const qc = useQueryClient();
  const setAuth = useAuthStore((s) => s.setAuth);
  const currentUser = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);
  const refreshToken = useAuthStore((s) => s.refreshToken);

  return useMutation({
    mutationFn: (body: UpdateProfileRequest) => meApi.updateProfile(body),

    onSuccess: (updatedUser) => {
      // 프로필 캐시 무효화
      qc.invalidateQueries({ queryKey: queryKeys.me.profile });
      // authStore의 user도 최신 정보로 동기화
      if (accessToken) {
        setAuth(updatedUser, accessToken, refreshToken ?? undefined);
      }
    },

    onError: (error) => {
      const axiosErr = error as AxiosError<{ message?: string }>;
      const message =
        axiosErr.response?.data?.message ?? "프로필 업데이트에 실패했습니다.";
      console.error("Update profile error:", message);
    },
  });
};

/**
 * 비밀번호 변경 뮤테이션
 * PATCH /users/me/password — currentPassword, newPassword만 전송
 */
export const useChangePasswordMutation = () => {
  return useMutation({
    mutationFn: (body: ChangePasswordRequest) => meApi.changePassword(body),

    onError: (error) => {
      const axiosErr = error as AxiosError<{ message?: string }>;
      const message =
        axiosErr.response?.data?.message ?? "비밀번호 변경에 실패했습니다.";
      console.error("Change password error:", message);
    },
  });
};

/**
 * 회원 탈퇴 뮤테이션
 * DELETE /users/me
 */
export const useWithdrawMutation = () => {
  const clear = useAuthStore((s) => s.clear);

  return useMutation({
    mutationFn: () => meApi.withdraw(),

    onSuccess: () => {
      // 탈퇴 완료 후 인증 정보 초기화
      clear();
    },

    onError: (error) => {
      const axiosErr = error as AxiosError<{ message?: string }>;
      const message =
        axiosErr.response?.data?.message ?? "회원 탈퇴에 실패했습니다.";
      console.error("Withdraw error:", message);
    },
  });
};
