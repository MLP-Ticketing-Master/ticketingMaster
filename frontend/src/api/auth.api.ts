import api from "@/lib/axios";
import type {
  AuthResponse,
  LoginRequest,
  LoginResponseRaw,
  SignupRequest,
  TokenRefreshResponse,
  User,
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

/**
 * 내 정보 조회 (Me)
 */
export async function getMe(): Promise<User> {
  const res = await api.get<User>("/auth/me");
  return res.data;
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
