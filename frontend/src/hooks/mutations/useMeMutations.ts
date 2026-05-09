import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  meApi,
  type ChangePasswordRequest,
  type UpdateProfileRequest,
} from "@/api";
import { queryKeys } from "@/lib/queryKeys";

const useMock = true;

export const useUpdateProfileMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: useMock
      ? async (body: UpdateProfileRequest) => body
      : (body: UpdateProfileRequest) => meApi.updateProfile(body),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: queryKeys.me.profile }),
  });
};

export const useChangePasswordMutation = () =>
  useMutation({
    mutationFn: useMock
      ? async (_: ChangePasswordRequest) => ({})
      : (body: ChangePasswordRequest) => meApi.changePassword(body),
  });
