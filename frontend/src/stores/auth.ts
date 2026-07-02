import { defineStore } from 'pinia';

import {
  AUTH_MODE,
  AUTH_PROVIDER,
  GOOGLE_CLIENT_ID,
  type AuthMode,
  type AuthProvider,
} from '@/auth/config';

export interface AuthUserDisplayInfo {
  displayName: string | null;
  email: string | null;
  avatarUrl: string | null;
}

export interface AuthSession {
  token: string;
  user: Partial<AuthUserDisplayInfo>;
}

interface AuthState {
  mode: AuthMode;
  provider: AuthProvider;
  googleClientId: string | null;
  token: string | null;
  user: AuthUserDisplayInfo | null;
  isLoading: boolean;
  isInitialized: boolean;
}

function normalizeUser(user: Partial<AuthUserDisplayInfo>): AuthUserDisplayInfo {
  return {
    displayName: user.displayName ?? null,
    email: user.email ?? null,
    avatarUrl: user.avatarUrl ?? null,
  };
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    mode: AUTH_MODE,
    provider: AUTH_PROVIDER,
    googleClientId: GOOGLE_CLIENT_ID,
    token: null,
    user: null,
    isLoading: false,
    isInitialized: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAuthRequiredMode: (state) => state.mode === 'oidc',
    canAccessProtectedRoutes(): boolean {
      return !this.isAuthRequiredMode || this.isAuthenticated;
    },
    userDisplayName: (state) => state.user?.displayName ?? state.user?.email ?? null,
    hasGoogleClientIdConfigured: (state) => Boolean(state.googleClientId),
    providerLabel: (state) => {
      if (state.provider === 'google') {
        return 'Google';
      }

      if (state.provider === 'local-demo') {
        return 'Local Demo';
      }

      return 'OIDC';
    },
  },
  actions: {
    initialize() {
      if (this.isInitialized) {
        return;
      }

      this.isLoading = true;

      // Future provider SDK bootstrap belongs here. The current foundation keeps auth in memory.
      this.isInitialized = true;
      this.isLoading = false;
    },
    setAuthMode(mode: AuthMode, provider?: AuthProvider) {
      this.mode = mode;
      this.provider = provider ?? (mode === 'local-demo' ? 'local-demo' : 'oidc');
    },
    setAuthenticatedSession(session: AuthSession) {
      this.token = session.token;
      this.user = normalizeUser(session.user);
      this.isInitialized = true;
      this.isLoading = false;
    },
    clearSession() {
      this.token = null;
      this.user = null;
    },
    handleUnauthorized() {
      this.clearSession();
    },
    logout() {
      this.clearSession();
      this.isInitialized = true;
      this.isLoading = false;
    },
  },
});
