import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/types";
import { authApi } from "@/api/endpoints";

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string, displayName?: string) => Promise<void>;
  logout: () => void;
  refresh: () => Promise<string | null>;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,

      async login(usernameOrEmail, password) {
        const res = await authApi.login(usernameOrEmail, password);
        set({ user: res.user, accessToken: res.accessToken, refreshToken: res.refreshToken });
      },

      async register(username, email, password, displayName) {
        const res = await authApi.register(username, email, password, displayName);
        set({ user: res.user, accessToken: res.accessToken, refreshToken: res.refreshToken });
      },

      logout() {
        set({ user: null, accessToken: null, refreshToken: null });
      },

      async refresh() {
        const rt = get().refreshToken;
        if (!rt) return null;
        try {
          const res = await authApi.refresh(rt);
          set({ user: res.user, accessToken: res.accessToken, refreshToken: res.refreshToken });
          return res.accessToken;
        } catch {
          set({ user: null, accessToken: null, refreshToken: null });
          return null;
        }
      },

      isAdmin() {
        return get().user?.role === "ADMIN";
      },
    }),
    { name: "prephub-auth" }
  )
);
