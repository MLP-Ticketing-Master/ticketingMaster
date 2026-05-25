import api from "@/lib/axios";
import type {
  ChangePasswordRequest,
  UpdateProfileRequest,
  User,
  UserResponse,
} from "@/types";

// 백엔드 UserResponse → 프론트 User 타입으로 변환
function toUser(res: UserResponse): User {
  return {
    id: res.userId,
    email: res.email,
    nickname: res.nickname,
    phone: res.phone,
    role: res.role,
  };
}

export async function getProfile(): Promise<User> {
  const res = await api.get<UserResponse>("/users/me");
  return toUser(res.data);
}

export async function updateProfile(body: UpdateProfileRequest): Promise<User> {
  // email 제외, nickname + phone만 전송
  const res = await api.patch<UserResponse>("/users/me", {
    nickname: body.nickname,
    phone: body.phone,
  });
  return toUser(res.data);
}

export async function changePassword(
  body: ChangePasswordRequest
): Promise<void> {
  // newPasswordConfirm은 프론트 검증용 — API 미전송
  await api.patch("/users/me/password", {
    currentPassword: body.currentPassword,
    newPassword: body.newPassword,
  });
}

export async function withdraw(): Promise<void> {
  await api.delete("/users/me");
}
