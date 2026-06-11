import { useMutation, useQueryClient } from "@tanstack/react-query";
import { changePassword, updateProfile, withdraw } from "@/api";
import type { ChangePasswordRequest, UpdateProfileRequest } from "@/types";
import { queryKeys } from "@/lib/queryKeys";
import { resolveErrorMessage } from "@/lib/error";
import { useAuthStore } from "@/store";

/**
 * 프로필 업데이트 뮤테이션
 * PATCH /users/me — nickname, phone만 수정 (email 변경 불가)
 */
export const useUpdateProfileMutation = () => {
  const qc = useQueryClient();
  const setAuth = useAuthStore((s) => s.setAuth);
  const accessToken = useAuthStore((s) => s.accessToken);
  const refreshToken = useAuthStore((s) => s.refreshToken);

  return useMutation({
    mutationFn: (body: UpdateProfileRequest) => updateProfile(body),

    onSuccess: (updatedUser) => {
      // 프로필 캐시 무효화
      qc.invalidateQueries({ queryKey: queryKeys.me.profile });
      // authStore의 user도 최신 정보로 동기화
      if (accessToken) {
        setAuth(updatedUser, accessToken, refreshToken ?? undefined);
      }
    },

    onError: (error) => {
      console.error(
        "Update profile error:",
        resolveErrorMessage(error, "프로필 업데이트에 실패했습니다."),
      );
    },
  });
};

/**
 * 비밀번호 변경 뮤테이션
 * PATCH /users/me/password — currentPassword, newPassword만 전송
 */
export const useChangePasswordMutation = () => {
  return useMutation({
    mutationFn: (body: ChangePasswordRequest) => changePassword(body),

    onError: (error) => {
      console.error(
        "Change password error:",
        resolveErrorMessage(error, "비밀번호 변경에 실패했습니다."),
      );
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
    mutationFn: () => withdraw(),

    onSuccess: () => {
      // 탈퇴 완료 후 인증 정보 초기화
      clear();
    },

    onError: (error) => {
      console.error(
        "Withdraw error:",
        resolveErrorMessage(error, "회원 탈퇴에 실패했습니다."),
      );
    },
  });
};
