import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import type { TokenResponse } from "../lib/types";
import { useAuthStore } from "./auth";

const user = {
  id: "18a1d559-a3c9-44a9-b75f-4aa856f055cb",
  email: "ana@example.com",
  displayName: "Ana",
  emailConfirmed: true,
  createdAt: "2026-11-01T12:00:00Z",
};

const tokenResponse: TokenResponse = {
  accessToken: "new-token",
  tokenType: "Bearer",
  expiresIn: 600,
  user,
};

function response(status: number, body: unknown): Response {
  return {
    status,
    ok: status >= 200 && status < 300,
    text: async () => JSON.stringify(body),
  } as Response;
}

beforeEach(() => {
  setActivePinia(createPinia());
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("auth store refresh", () => {
  it("shares one refresh request between concurrent callers", async () => {
    let resolveFetch: (value: Response) => void = () => {};
    const fetchMock = vi.fn(
      () =>
        new Promise<Response>((resolve) => {
          resolveFetch = resolve;
        }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const store = useAuthStore();
    const first = store.refreshSession();
    const second = store.refreshSession();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    resolveFetch(response(200, tokenResponse));

    await expect(first).resolves.toBe("new-token");
    await expect(second).resolves.toBe("new-token");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(store.isAuthenticated).toBe(true);
    expect(store.user?.email).toBe("ana@example.com");
  });

  it("clears the session when the refresh is rejected", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(response(401, { code: "AUTH_REFRESH_INVALID" })),
    );
    const store = useAuthStore();

    await expect(store.refreshSession()).resolves.toBeNull();
    expect(store.isAuthenticated).toBe(false);
    expect(store.user).toBeNull();
  });
});
