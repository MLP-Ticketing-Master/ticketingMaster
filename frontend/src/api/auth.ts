import axios from "axios";
import api from "@/lib/axios";
import type {
  AuthResponse,
  LoginRequest,
  LoginResponseRaw,
  SignupRequest,
  TokenRefreshResponse,
} from "@/types";

// 백엔드 평탄형 응답 → 프론트 정규화 형태(user 객체)로 매핑
function toAuthResponse(raw: LoginResponseRaw): AuthResponse {
  return {
    accessToken: raw.accessToken,
    refreshToken: raw.refreshToken,
    user: {
      id: raw.userId,
      nickname: raw.nickname,
      email: raw.email,
      phone: raw.phone,
      role: raw.role,
    },
  };
}

/**
 * 이메일 중복 확인
 * 사용 가능: 200 OK / 중복: 409 Conflict (DUPLICATE_EMAIL)
 * 인터셉터 간섭을 피하기 위해 순수 axios 사용
 */
export async function checkEmailDuplicate(email: string): Promise<boolean> {
  const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
  try {
    await axios.get(`${BASE_URL}/auth/check-email`, { params: { email } });
    return true; // 사용 가능
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 409) {
      return false; // 중복
    }
    // 네트워크 오류 등 예상치 못한 에러는 위로 던짐
    throw error;
  }
}

/**
 * 로그인
 */
export async function getLogin(body: LoginRequest): Promise<AuthResponse> {
  const res = await api.post<LoginResponseRaw>("/auth/login", body);
  return toAuthResponse(res.data);
}

// 회원가입 후 자동 로그인
export async function getSignup(body: SignupRequest): Promise<AuthResponse> {
  try {
    await api.post("/auth/signup", body);

    const loginRes = await api.post<LoginResponseRaw>("/auth/login", {
      email: body.email,
      password: body.password,
    });

    return toAuthResponse(loginRes.data);
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

/** refresh — body 에 refreshToken 필수 */
export async function getRefresh(
  refreshToken: string,
): Promise<TokenRefreshResponse> {
  const res = await api.post<TokenRefreshResponse>("/auth/refresh", {
    refreshToken,
  });
  return res.data;
}

/**
 * 비밀번호 재설정 링크 요청
 * POST /auth/password-reset/request
 */
export async function requestPasswordReset(email: string): Promise<void> {
  await api.post("/auth/password-reset/request", { email });
}

/**
 * 비밀번호 재설정 확인 (토큰 + 새 비밀번호)
 * POST /auth/password-reset/confirm
 */
export async function confirmPasswordReset(
  token: string,
  newPassword: string,
): Promise<void> {
  await api.post("/auth/password-reset/confirm", { token, newPassword });
}
