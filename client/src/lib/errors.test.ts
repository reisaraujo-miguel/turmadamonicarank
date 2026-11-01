import { describe, expect, it } from "vitest";

import { ApiError, fieldErrorFor, messageFor } from "./errors";

describe("messageFor", () => {
  it("maps known API error codes to pt-BR copy", () => {
    expect(messageFor(new ApiError(401, "AUTH_INVALID_CREDENTIALS"))).toBe(
      "E-mail ou senha não conferem.",
    );
    expect(messageFor(new ApiError(401, "AUTH_REFRESH_INVALID"))).toBe(
      "Sua sessão expirou. Entre de novo para continuar.",
    );
    expect(messageFor(new ApiError(409, "AUTH_EMAIL_ALREADY_REGISTERED"))).toBe(
      "Este e-mail já está cadastrado.",
    );
  });

  it("falls back to a generic message for unknown codes and non-API errors", () => {
    expect(messageFor(new ApiError(500, "SOMETHING_NEW"))).toBe(
      "Não foi possível concluir a ação. Tente de novo.",
    );
    expect(messageFor(new Error("boom"))).toBe("Não foi possível concluir a ação. Tente de novo.");
  });

  it("keeps network failures actionable", () => {
    expect(messageFor(new ApiError(0, "NETWORK_ERROR"))).toBe(
      "Não conseguimos falar com o servidor. Confira sua conexão.",
    );
  });
});

describe("fieldErrorFor", () => {
  it("returns the message for a matching field", () => {
    const error = new ApiError(422, "VALIDATION_FAILED", [
      { field: "email", code: "VALIDATION_FAILED", message: "must be a well-formed email" },
    ]);

    expect(fieldErrorFor(error, "email")).toBe("must be a well-formed email");
    expect(fieldErrorFor(error, "password")).toBeNull();
    expect(fieldErrorFor(new Error("boom"), "email")).toBeNull();
  });
});
