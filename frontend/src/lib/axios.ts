import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";

export const http = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  withCredentials: true,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("accessToken");
    }
    return Promise.reject(error);
  },
);
