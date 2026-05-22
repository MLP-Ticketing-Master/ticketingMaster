import axios, { AxiosError } from "axios";
import { useAuthStore } from "@/store";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

// 401 발생 시 토큰 갱신을 시도하지 않을 인증 자체 엔드포인트
const AUTH_ENDPOINTS = ["/auth/login", "/auth/signup", "/auth/refresh"];
const isAuthEndpoint = (url?: string) =>
  !!url && AUTH_ENDPOINTS.some((ep) => url.includes(ep));

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  withCredentials: true,
});

// ====== Request Interceptor ======
// 모든 요청에 Authorization 헤더 추가
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// ====== Response Interceptor ======
// 토큰 만료 시 자동 갱신
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: any) => void;
  reject: (reason?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  isRefreshing = false;
  failedQueue = [];
};

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const originalRequest = error.config as any;

    // 인증 엔드포인트(login/signup/refresh) 자체의 401은 갱신 시도 없이 그대로 전달
    // 잘못된 비밀번호 같은 도메인 에러를 자동 갱신이 덮어쓰지 못하도록 차단
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !isAuthEndpoint(originalRequest?.url)
    ) {
      if (isRefreshing) {
        // 이미 갱신 중이면 완료될 때까지 대기
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // 토큰 갱신 API 호출 — 응답에 user 정보 없음(refresh 전용)
        const response = await axios.post<{
          accessToken: string;
          refreshToken: string;
        }>(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true });

        const { accessToken } = response.data;

        // 기존 user 유지하면서 토큰만 갱신
        const currentUser = useAuthStore.getState().user;
        if (currentUser) {
          useAuthStore.getState().setAuth(currentUser, accessToken);
        } else {
          useAuthStore.getState().clear();
        }

        // 새 토큰으로 요청 헤더 업데이트
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;

        // 대기 중인 요청들 처리
        processQueue(null, accessToken);

        // 원본 요청 재시도
        return api(originalRequest);
      } catch (refreshError) {
        // 갱신 실패 시 로그아웃
        useAuthStore.getState().clear();
        processQueue(refreshError, null);
        return Promise.reject(refreshError);
      }
    }

    // 401이지만 갱신 대상이 아닌 경우(인증 엔드포인트 자체)는 원본 에러 그대로 전달
    return Promise.reject(error);
  },
);

export default api;
