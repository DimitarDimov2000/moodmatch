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

export const AUTH_SESSION_STORAGE_KEY = 'moodmatch.auth.session';

interface AuthState {
  mode: AuthMode;
  provider: AuthProvider;
  token: string | null;
  user: AuthUserDisplayInfo | null;
  isLoading: boolean;
  isInitialized: boolean;
  providerSetupState: AuthProviderSetupState;
  providerError: string | null;
  initializationPromise: Promise<void> | null;
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

function normalizeSession(session: AuthSession): AuthSession {
  return {
    token: session.token,
    user: normalizeUser(session.user),
  };
}

function getStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function persistSession(session: AuthSession) {
  try {
    getStorage()?.setItem(AUTH_SESSION_STORAGE_KEY, JSON.stringify(normalizeSession(session)));
  } catch {
    // Demo persistence is best-effort; auth state still stays in memory for the current tab.
  }
}

function clearPersistedSession() {
  try {
    getStorage()?.removeItem(AUTH_SESSION_STORAGE_KEY);
  } catch {
    // Ignore storage cleanup failures and still clear the in-memory session.
  }
}

function parseStoredSession(rawValue: string): AuthSession | null {
  try {
    const parsed = JSON.parse(rawValue) as Partial<AuthSession> | null;

    if (!parsed || typeof parsed !== 'object' || typeof parsed.token !== 'string') {
      return null;
    }

    return normalizeSession({
      token: parsed.token,
      user: typeof parsed.user === 'object' && parsed.user !== null ? parsed.user : {},
    });
  } catch {
    return null;
  }
}

function readStoredSession(): AuthSession | null {
  const rawValue = getStorage()?.getItem(AUTH_SESSION_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  const session = parseStoredSession(rawValue);

  if (!session) {
    clearPersistedSession();
  }

  return session;
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
    initializationPromise: null,
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
    initialize(): Promise<void> {
      if (this.isInitialized) {
        return Promise.resolve();
      }

      if (this.initializationPromise) {
        return this.initializationPromise;
      }

      this.providerError = null;
      this.isLoading = true;
      this.providerSetupState = this.isAuthRequiredMode ? 'ready' : 'not-needed';

      this.initializationPromise = this.restoreSession()
        .finally(() => {
          this.initializationPromise = null;
        });

      return this.initializationPromise;
    },
    setAuthMode(mode: AuthMode, provider?: AuthProvider) {
      this.mode = mode;
      this.provider = provider ?? mode;
      this.providerSetupState = 'idle';
      this.providerError = null;
      this.isInitialized = false;
      this.isLoading = false;
      this.initializationPromise = null;
    },
    setAuthenticatedSession(session: AuthSession, persist = true) {
      const normalizedSession = normalizeSession(session);

      this.token = normalizedSession.token;
      this.user = normalizeUser(normalizedSession.user);
      this.providerSetupState = this.isAuthRequiredMode ? 'ready' : this.providerSetupState;
      this.providerError = null;
      this.isInitialized = true;
      this.isLoading = false;

      if (persist) {
        persistSession(normalizedSession);
      }
    },
    clearSession() {
      this.token = null;
      this.user = null;
      clearPersistedSession();
    },
    async restoreSession() {
      if (!this.isAuthRequiredMode) {
        this.isInitialized = true;
        this.isLoading = false;
        return;
      }

      const storedSession = readStoredSession();

      if (!storedSession) {
        this.clearSession();
        this.isInitialized = true;
        this.isLoading = false;
        return;
      }

      this.token = storedSession.token;
      this.user = normalizeUser(storedSession.user);

      try {
        const verifiedUser = normalizeUser(await getCurrentUser());
        this.user = verifiedUser;
        persistSession({
          token: storedSession.token,
          user: verifiedUser,
        });
      } catch {
        this.clearSession();
      } finally {
        this.isInitialized = true;
        this.isLoading = false;
      }
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
        const refreshedUser = normalizeUser(await getCurrentUser());
        this.user = refreshedUser;
        persistSession({
          token: this.token,
          user: refreshedUser,
        });
        return true;
      } catch {
        this.clearSession();
        return false;
      }
    },
    handleUnauthorized() {
      this.clearSession();
      this.isInitialized = true;
      this.isLoading = false;
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
