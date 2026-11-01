import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { isApiError } from "../lib/errors";
import { api, setHttpAccessToken, setRefreshHandler, setSessionExpiredHandler } from "../lib/http";
import type { RegisterResponse, TokenResponse, UserSelf } from "../lib/types";

type AuthStatus = "restoring" | "anonymous" | "authenticated";

export const useAuthStore = defineStore("auth", () => {
  const user = ref<UserSelf | null>(null);
  const accessToken = ref<string | null>(null);
  const status = ref<AuthStatus>("restoring");

  let initializePromise: Promise<void> | null = null;
  let refreshPromise: Promise<string | null> | null = null;

  const isAuthenticated = computed(() => status.value === "authenticated" && user.value !== null);
  const emailConfirmed = computed(() => user.value?.emailConfirmed === true);
  const displayName = computed(() => user.value?.displayName ?? user.value?.email ?? "");
  const initials = computed(() => {
    const source = user.value?.displayName || user.value?.email || "";
    return source
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join("");
  });

  function applySession(response: TokenResponse): void {
    user.value = response.user;
    accessToken.value = response.accessToken;
    status.value = "authenticated";
    setHttpAccessToken(response.accessToken);
  }

  function clearSession(): void {
    user.value = null;
    accessToken.value = null;
    status.value = "anonymous";
    setHttpAccessToken(null);
  }

  async function refreshSession(): Promise<string | null> {
    if (refreshPromise) {
      return refreshPromise;
    }
    refreshPromise = (async () => {
      try {
        const response = await api.post<TokenResponse>("/auth/refresh", undefined, {
          skipAuth: true,
          skipRefresh: true,
        });
        applySession(response);
        return response.accessToken;
      } catch (error) {
        if (isApiError(error) && error.status === 401) {
          clearSession();
          return null;
        }
        clearSession();
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
    return refreshPromise;
  }

  async function initialize(): Promise<void> {
    if (status.value !== "restoring") {
      return;
    }
    if (initializePromise) {
      return initializePromise;
    }
    initializePromise = (async () => {
      await refreshSession();
      if (status.value === "restoring") {
        status.value = "anonymous";
      }
    })();
    return initializePromise;
  }

  async function login(email: string, password: string): Promise<void> {
    const response = await api.post<TokenResponse>(
      "/auth/login",
      { email, password },
      { skipAuth: true, skipRefresh: true },
    );
    applySession(response);
  }

  async function register(
    email: string,
    password: string,
    displayName: string | null,
  ): Promise<UserSelf> {
    const response = await api.post<RegisterResponse>(
      "/auth/register",
      { email, password, displayName },
      { skipAuth: true, skipRefresh: true },
    );
    return response.user;
  }

  async function confirmEmail(token: string): Promise<void> {
    await api.post<void>("/auth/confirm-email", { token }, { skipAuth: true, skipRefresh: true });
  }

  async function resendConfirmation(email: string): Promise<void> {
    await api.post<void>(
      "/auth/resend-confirmation",
      { email },
      { skipAuth: true, skipRefresh: true },
    );
  }

  async function requestPasswordReset(email: string): Promise<void> {
    await api.post<void>("/auth/forgot-password", { email }, { skipAuth: true, skipRefresh: true });
  }

  async function resetPassword(token: string, password: string): Promise<void> {
    await api.post<void>(
      "/auth/reset-password",
      { token, password },
      { skipAuth: true, skipRefresh: true },
    );
  }

  async function loadMe(): Promise<void> {
    if (!accessToken.value) {
      return;
    }
    try {
      user.value = await api.get<UserSelf>("/auth/me");
    } catch (error) {
      if (isApiError(error) && error.status === 401) {
        clearSession();
      }
    }
  }

  async function logout(): Promise<void> {
    try {
      await api.post<void>("/auth/logout", undefined, {
        skipAuth: true,
        skipRefresh: true,
      });
    } finally {
      clearSession();
    }
  }

  setRefreshHandler(refreshSession);
  setSessionExpiredHandler(clearSession);
  setHttpAccessToken(null);

  return {
    user,
    status,
    isAuthenticated,
    emailConfirmed,
    displayName,
    initials,
    initialize,
    login,
    register,
    confirmEmail,
    resendConfirmation,
    requestPasswordReset,
    resetPassword,
    loadMe,
    logout,
    refreshSession,
  };
});
