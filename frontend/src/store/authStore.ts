import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/types";

interface AuthState {
  user: User | null;
  accessToken: string | null;
  setAuth: (user: User, accessToken: string) => void;
  clear: () => void;
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      setAuth: (user, accessToken) => {
        localStorage.setItem("accessToken", accessToken);
        set({ user, accessToken });
      },
      clear: () => {
        localStorage.removeItem("accessToken");
        set({ user: null, accessToken: null });
      },
      isAuthenticated: () => !!get().accessToken,
      isAdmin: () => get().user?.role === "ADMIN",
    }),
    { name: "auth-storage" },
  ),
);
