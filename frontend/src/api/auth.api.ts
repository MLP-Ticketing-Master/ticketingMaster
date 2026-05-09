import { http } from "@/lib/axios";
import type {
  AuthResponse,
  LoginRequest,
  SignupRequest,
  User,
} from "@/types";

export const authApi = {
  login: (body: LoginRequest) =>
    http.post<AuthResponse>("/auth/login", body).then((r) => r.data),
  signup: (body: SignupRequest) =>
    http.post<AuthResponse>("/auth/signup", body).then((r) => r.data),
  logout: () => http.post("/auth/logout").then((r) => r.data),
  me: () => http.get<User>("/auth/me").then((r) => r.data),
};
