export type UserRole = "USER" | "ADMIN";

export interface User {
  id: number;
  nickname: string;
  email: string;
  phone: string;
  role: UserRole;
  joinedAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  nickname: string;
  email: string;
  phone: string;
  password: string;
}

// 백엔드 /auth/login 원본 응답 (평탄형)
export interface LoginResponseRaw {
  accessToken: string;
  refreshToken: string;
  userId: number;
  email: string;
  nickname: string;
  phone: string;
  role: UserRole;
}

// 프론트 내부에서 사용하는 정규화 응답
export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  user: User;
}

// /auth/refresh 응답 (user 정보 없음)
export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  role: UserRole;
}
