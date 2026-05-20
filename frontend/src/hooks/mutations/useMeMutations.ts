import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  meApi,
  type ChangePasswordRequest,
  type UpdateProfileRequest,
} from "@/api";
import { queryKeys } from "@/lib/queryKeys";

// 환경변수 기반 모의 데이터 사용 여부
const useMock = import.meta.env.VITE_USE_MOCK === 'true';


/**
 * 프로필 업데이트 뮤테이션
 */
export const useUpdateProfileMutation = () => {
  const qc = useQueryClient();
  
  return useMutation({
    mutationFn: async (body: UpdateProfileRequest) => {
      if (useMock) {
        return body;
      }
      return meApi.updateProfile(body);
    },
    onSuccess: () => {
      // 프로필 캐시 무효화
      qc.invalidateQueries({ queryKey: queryKeys.me.profile });
    },

    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        "프로필 업데이트 실패";
 
      console.error("Update profile error:", message);
    },
  });
};

/**
 * 비밀번호 변경 뮤테이션
 */
export const useChangePasswordMutation = () => {
  return useMutation({
    mutationFn: async (body: ChangePasswordRequest) => {
      if (useMock) {
        return undefined;
      }
      return meApi.changePassword(body);
    },
 
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        "비밀번호 변경 실패";
 
      console.error("Change password error:", message);
    },
  });
};

/**
 * 회원 탈퇴 뮤테이션
 */
export const useWithdrawMutation = () => {
  return useMutation({
    mutationFn: async () => {
      if (useMock) {
        return undefined;
      }
      return meApi.withdraw();
    },
 
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        "회원 탈퇴 실패";
 
      console.error("Withdraw error:", message);
    },
  });
};