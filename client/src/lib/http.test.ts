import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "./errors";
import { api, setHttpAccessToken, setRefreshHandler, setSessionExpiredHandler } from "./http";

function response(status: number, body: unknown): Response {
  return {
    status,
    ok: status >= 200 && status < 300,
    text: async () => JSON.stringify(body),
  } as Response;
}

beforeEach(() => {
  setHttpAccessToken(null);
  setRefreshHandler(async () => null);
  setSessionExpiredHandler(() => {});
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("api request handling", () => {
  it("maps problem+json into an ApiError with field errors", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      response(422, {
        code: "VALIDATION_FAILED",
        detail: "One or more fields are invalid.",
        errors: [{ field: "email", code: "VALIDATION_FAILED", message: "must be an email" }],
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const request = api.post("/anything", { email: "x" });

    await expect(request).rejects.toMatchObject({
      name: "ApiError",
      status: 422,
      code: "VALIDATION_FAILED",
      fieldErrors: [{ field: "email" }],
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("refreshes once and retries with the new bearer token", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(401, { code: "AUTH_REQUIRED" }))
      .mockResolvedValueOnce(response(200, { ok: true }));
    vi.stubGlobal("fetch", fetchMock);
    setHttpAccessToken("old-token");
    const refresh = vi.fn(async () => {
      setHttpAccessToken("new-token");
      return "new-token";
    });
    setRefreshHandler(refresh);

    await expect(api.get<{ ok: boolean }>("/protected")).resolves.toEqual({ ok: true });

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const secondCall = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(new Headers(secondCall[1].headers).get("Authorization")).toBe("Bearer new-token");
  });

  it("notifies the session handler and throws when refresh fails", async () => {
    const fetchMock = vi.fn().mockResolvedValue(response(401, { code: "AUTH_REQUIRED" }));
    vi.stubGlobal("fetch", fetchMock);
    setHttpAccessToken("old-token");
    const expired = vi.fn();
    setSessionExpiredHandler(expired);
    setRefreshHandler(async () => null);

    await expect(api.get("/protected")).rejects.toBeInstanceOf(ApiError);
    expect(expired).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not refresh auth calls that opt out", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(response(401, { code: "AUTH_INVALID_CREDENTIALS" }));
    vi.stubGlobal("fetch", fetchMock);
    setHttpAccessToken("old-token");
    const refresh = vi.fn(async () => "new-token");
    setRefreshHandler(refresh);

    await expect(
      api.post(
        "/auth/login",
        { email: "ana@example.com", password: "wrong" },
        { skipAuth: true, skipRefresh: true },
      ),
    ).rejects.toMatchObject({ status: 401, code: "AUTH_INVALID_CREDENTIALS" });

    expect(refresh).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
