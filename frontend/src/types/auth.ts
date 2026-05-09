export type UserRole = "USER" | "ADMIN";

export interface User {
  id: number;
  name: string;
  email: string;
  phone: string;
  joinedAt: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  passwordConfirm: string;
}

export interface AuthResponse {
  accessToken: string;
  user: User;
}
