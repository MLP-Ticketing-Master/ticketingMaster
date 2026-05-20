import api from "@/lib/axios";
import type {
  AuthResponse,
  LoginRequest,
  SignupRequest,
  User,
} from "@/types";

/**
 * 로그인
 */
export async function getLogin(body: LoginRequest): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>("/auth/login", body);
  return res.data;
}

// 회원가입 후 자동 로그인
export async function getSignup(body: SignupRequest): Promise<AuthResponse> {
  try {
    await api.post("/auth/signup", body);  // 회원가입
    
    const loginRes = await api.post<AuthResponse>("/auth/login", {
      email: body.email,
      password: body.password,
    });  // 자동 로그인
    
    return loginRes.data;
  } catch (error) {
      console.error("회원가입 또는 로그인 실패:", error);
      throw error;
    }
}

/**
 * 로그아웃
 */
export async function getLogout(): Promise<void> {
  await api.post("/auth/logout");
}

/**
 * 내 정보 조회 (Me)
 */
export async function getMe(): Promise<User> {
  const res = await api.get<User>("/auth/me");
  return res.data;
}
/** refresh */
export async function getRefresh(): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>("/auth/refresh");
  return res.data;
}