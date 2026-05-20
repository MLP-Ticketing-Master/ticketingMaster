import axios,{ AxiosError } from "axios";
import { useAuthStore } from "@/store";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

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

    // 401 에러 && 재시도하지 않은 요청
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 이미 갱신 중이면 완료될 때까지 대기
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }
 
      originalRequest._retry = true;
      isRefreshing = true;
 
      try {
        // 토큰 갱신 API 호출
        const response = await axios.post<{
          accessToken: string;
          user: any;
        }>(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true });
 
        const { accessToken, user } = response.data;
 
        // 새 토큰 저장
        useAuthStore.getState().setAuth(user, accessToken);
 
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
 
    // 401이 아닌 다른 에러는 그대로 반환
    if (error.response?.status === 401) {
      // 토큰 관련 에러이지만 재시도했는데도 실패
      useAuthStore.getState().clear();
    }
 
    return Promise.reject(error);
  }
);

export default api;
