import api from "@/lib/axios";
import type { User } from "@/types";

export interface UpdateProfileRequest {
  nickname: string;
  email: string;
  phone: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  newPasswordConfirm: string;
}

export async function getProfile(): Promise<User> {
  const res = await api.get<User>("/me");
  return res.data;
}

export async function updateProfile(body: UpdateProfileRequest): Promise<User> {
  const res = await api.put<User>("/me", body);
  return res.data;
}

export async function changePassword(body: ChangePasswordRequest): Promise<void> {
  await api.put("/me/password", body);
}

export async function withdraw(): Promise<void> {
  await api.delete("/me");
}


// API 객체로 내보내기
export const meApi = {
  profile: getProfile,
  updateProfile,
  changePassword,
  withdraw,
};