import { getJson, postJson } from './client';

export interface AuthUserResponse {
  id: string;
  email: string;
  displayName: string | null;
}

export interface AuthResponse {
  token: string;
  user: AuthUserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName?: string | null;
}

export function login(request: LoginRequest): Promise<AuthResponse> {
  return postJson<AuthResponse, LoginRequest>('/auth/login', request);
}

export function register(request: RegisterRequest): Promise<AuthResponse> {
  return postJson<AuthResponse, RegisterRequest>('/auth/register', request);
}

export function getCurrentUser(): Promise<AuthUserResponse> {
  return getJson<AuthUserResponse>('/auth/me');
}

export function logout(): Promise<void> {
  return postJson<null, null>('/auth/logout', null).then(() => undefined);
}
