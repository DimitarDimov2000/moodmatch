import { defineStore } from 'pinia';

import { getCurrentUser, login, logout as logoutRequest, register } from '@/api/auth';
import { ApiRequestError } from '@/api/client';
import {
  AUTH_MODE,
  AUTH_PROVIDER,
  type AuthMode,
  type AuthProvider,
} from '@/auth/config';

export interface AuthUserDisplayInfo {
  id: string | null;
  displayName: string | null;
  email: string | null;
}

export interface AuthSession {
  token: string;
  user: Partial<AuthUserDisplayInfo>;
}

export type AuthProviderSetupState = 'idle' | 'not-needed' | 'ready' | 'error';

interface AuthState {
  mode: AuthMode;
  provider: AuthProvider;
  token: string | null;
  user: AuthUserDisplayInfo | null;
  isLoading: boolean;
  isInitialized: boolean;
  providerSetupState: AuthProviderSetupState;
  providerError: string | null;
}

function normalizeUser(user: Partial<AuthUserDisplayInfo>): AuthUserDisplayInfo {
  return {
    id: user.id ?? null,
    displayName: user.displayName ?? null,
    email: user.email ?? null,
  };
}

function toAuthErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return fallback;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    mode: AUTH_MODE,
    provider: AUTH_PROVIDER,
    token: null,
    user: null,
    isLoading: false,
    isInitialized: false,
    providerSetupState: 'idle',
    providerError: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAuthRequiredMode: (state) => state.mode === 'local-password',
    canAccessProtectedRoutes(): boolean {
      return !this.isAuthRequiredMode || this.isAuthenticated;
    },
    userDisplayName: (state) => state.user?.displayName ?? state.user?.email ?? null,
    providerLabel: (state) => (state.provider === 'local-demo' ? 'Local Demo' : 'Email/password'),
  },
  actions: {
    initialize() {
      if (this.isInitialized || this.isLoading) {
        return;
      }

      this.providerSetupState = this.isAuthRequiredMode ? 'ready' : 'not-needed';
      this.providerError = null;
      this.isInitialized = true;
      this.isLoading = false;
    },
    setAuthMode(mode: AuthMode, provider?: AuthProvider) {
      this.mode = mode;
      this.provider = provider ?? mode;
      this.providerSetupState = 'idle';
      this.providerError = null;
      this.isInitialized = false;
      this.isLoading = false;
    },
    setAuthenticatedSession(session: AuthSession) {
      this.token = session.token;
      this.user = normalizeUser(session.user);
      this.providerSetupState = this.isAuthRequiredMode ? 'ready' : this.providerSetupState;
      this.providerError = null;
      this.isInitialized = true;
      this.isLoading = false;
    },
    clearSession() {
      this.token = null;
      this.user = null;
    },
    async loginWithPassword(email: string, password: string): Promise<boolean> {
      this.isLoading = true;
      this.providerError = null;

      try {
        this.setAuthenticatedSession(await login({ email, password }));
        return true;
      } catch (error) {
        this.providerError = toAuthErrorMessage(error, 'Login failed.');
        this.isLoading = false;
        return false;
      }
    },
    async registerWithPassword(email: string, password: string, displayName?: string): Promise<boolean> {
      this.isLoading = true;
      this.providerError = null;

      try {
        this.setAuthenticatedSession(
          await register({
            email,
            password,
            displayName: displayName?.trim() ? displayName.trim() : null,
          }),
        );
        return true;
      } catch (error) {
        this.providerError = toAuthErrorMessage(error, 'Account creation failed.');
        this.isLoading = false;
        return false;
      }
    },
    async refreshCurrentUser(): Promise<boolean> {
      if (!this.token) {
        return false;
      }

      try {
        this.user = normalizeUser(await getCurrentUser());
        return true;
      } catch {
        this.clearSession();
        return false;
      }
    },
    handleUnauthorized() {
      this.clearSession();
    },
    async logout() {
      if (this.token) {
        try {
          await logoutRequest();
        } catch {
          // The local session is already cleared; backend logout is best-effort for stale tokens.
        }
      }

      this.clearSession();
      this.isInitialized = true;
      this.isLoading = false;
    },
  },
});
