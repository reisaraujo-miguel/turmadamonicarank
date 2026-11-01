export interface UserSelf {
  id: string;
  email: string;
  displayName: string | null;
  emailConfirmed: boolean;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: UserSelf;
}

export interface RegisterResponse {
  user: UserSelf;
}

export interface ApiFieldError {
  field: string;
  code: string;
  message: string;
}

export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: ApiFieldError[];
}
