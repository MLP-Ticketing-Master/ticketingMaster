// GET /users/me 응답 (백엔드 UserResponse)
export interface UserResponse {
  userId: number;
  email: string;
  nickname: string;
  phone: string;
  role: "USER" | "ADMIN";
}

// PATCH /users/me — nickname, phone만 수정 가능 (email 변경 불가)
export interface UpdateProfileRequest {
  nickname: string;
  phone: string;
}

// PATCH /users/me/password — newPasswordConfirm 없음 (백엔드 검증 없음)
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  newPasswordConfirm: string; // 프론트 전용 검증용 (API에는 미전송)
}
