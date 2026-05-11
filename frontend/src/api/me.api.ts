import { http } from "@/lib/axios";
import type { User } from "@/types";

export interface UserStats {
  totalBookings: number;
  upcomingMatches: number;
  watchedMatches: number;
}

export interface UpdateProfileRequest {
  name: string;
  email: string;
  phone: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  newPasswordConfirm: string;
}

export const meApi = {
  profile: () => http.get<User>("/me").then((r) => r.data),
  stats: () => http.get<UserStats>("/me/stats").then((r) => r.data),
  updateProfile: (body: UpdateProfileRequest) =>
    http.put<User>("/me", body).then((r) => r.data),
  changePassword: (body: ChangePasswordRequest) =>
    http.put("/me/password", body).then((r) => r.data),
  withdraw: () => http.delete("/me").then((r) => r.data),
};
