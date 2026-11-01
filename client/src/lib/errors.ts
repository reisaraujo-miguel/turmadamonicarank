import type { ApiFieldError } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: ApiFieldError[];

  constructor(status: number, code: string, fieldErrors: ApiFieldError[] = [], message = "") {
    super(message || code);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

const MESSAGES: Record<string, string> = {
  VALIDATION_FAILED: "Confira os campos e tente de novo.",
  AUTH_INVALID_CREDENTIALS: "E-mail ou senha não conferem.",
  AUTH_EMAIL_NOT_CONFIRMED: "Confirme seu e-mail para votar.",
  AUTH_ACCOUNT_DISABLED: "Esta conta está desativada.",
  AUTH_TOKEN_INVALID: "Este link expirou ou já foi usado. Peça um novo.",
  AUTH_REFRESH_INVALID: "Sua sessão expirou. Entre de novo para continuar.",
  AUTH_REQUIRED: "Entre na sua conta para continuar.",
  AUTH_EMAIL_ALREADY_REGISTERED: "Este e-mail já está cadastrado.",
  FORBIDDEN: "Não foi possível confirmar esta ação.",
  NOT_FOUND: "Não encontramos o que você procurava.",
  METHOD_NOT_ALLOWED: "Esta ação não está disponível aqui.",
  RATE_LIMITED: "Muitas tentativas. Espere um pouco e tente de novo.",
  INTERNAL_ERROR: "Algo deu errado do nosso lado. Tente de novo.",
  NETWORK_ERROR: "Não conseguimos falar com o servidor. Confira sua conexão.",
  NETWORK_TIMEOUT: "O servidor demorou para responder. Tente de novo.",
  HTTP_ERROR: "Não foi possível concluir a ação. Tente de novo.",
};

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function messageFor(error: unknown): string {
  if (isApiError(error)) {
    return MESSAGES[error.code] ?? MESSAGES.HTTP_ERROR;
  }
  return MESSAGES.HTTP_ERROR;
}

export function fieldErrorFor(error: unknown, field: string): string | null {
  if (!isApiError(error)) {
    return null;
  }
  return error.fieldErrors.find((item) => item.field === field)?.message ?? null;
}
