import { ApiError } from "./errors";
import type { ApiProblem } from "./types";

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? "/api/v1").replace(/\/+$/, "");
const REQUEST_TIMEOUT_MS = 15_000;

interface RequestOptions {
  skipAuth?: boolean;
  skipRefresh?: boolean;
}

let accessToken: string | null = null;
let refreshHandler: (() => Promise<string | null>) | null = null;
let expiredHandler: (() => void) | null = null;

export function setHttpAccessToken(token: string | null): void {
  accessToken = token;
}

export function setRefreshHandler(handler: () => Promise<string | null>): void {
  refreshHandler = handler;
}

export function setSessionExpiredHandler(handler: () => void): void {
  expiredHandler = handler;
}

async function execute<T>(path: string, init: RequestInit, options: RequestOptions): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (!options.skipAuth && accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers,
      credentials: "same-origin",
      signal: controller.signal,
    });

    if (response.status === 401 && !options.skipAuth && !options.skipRefresh && refreshHandler) {
      const newToken = await refreshHandler();
      if (newToken) {
        headers.set("Authorization", `Bearer ${newToken}`);
        response = await fetch(`${API_BASE}${path}`, {
          ...init,
          headers,
          credentials: "same-origin",
          signal: controller.signal,
        });
      } else {
        expiredHandler?.();
      }
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiError(0, "NETWORK_TIMEOUT");
    }
    throw new ApiError(0, "NETWORK_ERROR");
  } finally {
    window.clearTimeout(timeout);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      throw new ApiError(response.status, "HTTP_ERROR");
    }
  }

  if (!response.ok) {
    const problem = data as ApiProblem | null;
    throw new ApiError(
      response.status,
      problem?.code ?? "HTTP_ERROR",
      problem?.errors ?? [],
      problem?.detail ?? "",
    );
  }

  return data as T;
}

export const api = {
  get<T>(path: string, options: RequestOptions = {}): Promise<T> {
    return execute<T>(path, { method: "GET" }, options);
  },

  post<T>(path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
    return execute<T>(
      path,
      {
        method: "POST",
        body: body === undefined ? undefined : JSON.stringify(body),
      },
      options,
    );
  },

  put<T>(path: string, body: unknown, options: RequestOptions = {}): Promise<T> {
    return execute<T>(path, { method: "PUT", body: JSON.stringify(body) }, options);
  },

  delete<T>(path: string, options: RequestOptions = {}): Promise<T> {
    return execute<T>(path, { method: "DELETE" }, options);
  },
};
