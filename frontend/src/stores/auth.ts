import { defineStore } from 'pinia';

import {
  AUTH_MODE,
  AUTH_PROVIDER,
  GOOGLE_CLIENT_ID,
  type AuthMode,
  type AuthProvider,
} from '@/auth/config';
import {
  googleIdentityProvider,
  type GoogleCredentialResponse,
  type GoogleIdentityInitializeResult,
} from '@/auth/google-identity';

export interface AuthUserDisplayInfo {
  displayName: string | null;
  email: string | null;
  avatarUrl: string | null;
}

export interface AuthSession {
  token: string;
  user: Partial<AuthUserDisplayInfo>;
}

export type AuthProviderSetupState =
  | 'idle'
  | 'not-needed'
  | 'loading'
  | 'ready'
  | 'missing-client-id'
  | 'error';

interface AuthState {
  mode: AuthMode;
  provider: AuthProvider;
  googleClientId: string | null;
  token: string | null;
  user: AuthUserDisplayInfo | null;
  isLoading: boolean;
  isInitialized: boolean;
  providerSetupState: AuthProviderSetupState;
  providerError: string | null;
}

function normalizeUser(user: Partial<AuthUserDisplayInfo>): AuthUserDisplayInfo {
  return {
    displayName: user.displayName ?? null,
    email: user.email ?? null,
    avatarUrl: user.avatarUrl ?? null,
  };
}

function decodeBase64Url(value: string): string | null {
  const normalizedValue = value.replace(/-/g, '+').replace(/_/g, '/');
  const paddedValue = normalizedValue.padEnd(normalizedValue.length + ((4 - normalizedValue.length % 4) % 4), '=');

  try {
    const binary = atob(paddedValue);
    const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
    return new TextDecoder().decode(bytes);
  } catch {
    return null;
  }
}

function parseCredentialPayload(token: string): Record<string, unknown> | null {
  const [, encodedPayload] = token.split('.');

  if (!encodedPayload) {
    return null;
  }

  const decodedPayload = decodeBase64Url(encodedPayload);
  if (!decodedPayload) {
    return null;
  }

  try {
    const parsedPayload = JSON.parse(decodedPayload);
    return parsedPayload && typeof parsedPayload === 'object'
      ? (parsedPayload as Record<string, unknown>)
      : null;
  } catch {
    return null;
  }
}

function readOptionalString(
  payload: Record<string, unknown> | null,
  key: string,
): string | null {
  const value = payload?.[key];
  return typeof value === 'string' && value.trim() ? value : null;
}

function extractUserFromCredentialToken(token: string): Partial<AuthUserDisplayInfo> {
  // Claims are only used for optional UI display data; backend token validation stays authoritative.
  const payload = parseCredentialPayload(token);

  return {
    displayName: readOptionalString(payload, 'name'),
    email: readOptionalString(payload, 'email'),
    avatarUrl: readOptionalString(payload, 'picture'),
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
    providerSetupState: 'idle',
    providerError: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAuthRequiredMode: (state) => state.mode === 'oidc',
    canAccessProtectedRoutes(): boolean {
      return !this.isAuthRequiredMode || this.isAuthenticated;
    },
    userDisplayName: (state) => state.user?.displayName ?? state.user?.email ?? null,
    hasGoogleClientIdConfigured: (state) => Boolean(state.googleClientId),
    isGoogleProvider: (state) => state.provider === 'google',
    needsGoogleClientId(): boolean {
      return this.isAuthRequiredMode && this.isGoogleProvider && !this.hasGoogleClientIdConfigured;
    },
    hasGoogleLoginOption(): boolean {
      return this.isAuthRequiredMode && this.isGoogleProvider && this.hasGoogleClientIdConfigured;
    },
    canStartGoogleLogin(): boolean {
      return this.hasGoogleLoginOption && this.providerSetupState === 'ready' && !this.isLoading;
    },
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
      if (this.isInitialized || this.isLoading) {
        return;
      }

      this.isLoading = true;

      if (!this.isAuthRequiredMode) {
        this.providerSetupState = 'not-needed';
        this.providerError = null;
        this.isInitialized = true;
        this.isLoading = false;
        return;
      }

      if (!this.isGoogleProvider) {
        this.providerSetupState = 'error';
        this.providerError = 'This frontend build only wires Google Identity Services for OIDC login.';
        this.isInitialized = true;
        this.isLoading = false;
        return;
      }

      void this.initializeGoogleProvider();
    },
    setAuthMode(mode: AuthMode, provider?: AuthProvider) {
      this.mode = mode;
      this.provider = provider ?? (mode === 'local-demo' ? 'local-demo' : 'oidc');
      this.providerSetupState = 'idle';
      this.providerError = null;
      this.isInitialized = false;
      this.isLoading = false;
    },
    setGoogleClientId(clientId: string | null) {
      this.googleClientId = clientId?.trim() ? clientId.trim() : null;
      this.providerSetupState = 'idle';
      this.providerError = null;
      this.isInitialized = false;
    },
    setAuthenticatedSession(session: AuthSession) {
      this.token = session.token;
      this.user = normalizeUser(session.user);
      this.providerSetupState = this.isGoogleProvider && this.isAuthRequiredMode ? 'ready' : this.providerSetupState;
      this.providerError = null;
      this.isInitialized = true;
      this.isLoading = false;
    },
    clearSession() {
      this.token = null;
      this.user = null;
    },
    async initializeGoogleProvider(): Promise<boolean> {
      this.providerSetupState = 'loading';
      this.providerError = null;

      const result = await googleIdentityProvider.initialize({
        clientId: this.googleClientId,
        onCredential: (response) => {
          this.handleGoogleCredentialResponse(response);
        },
      });

      return this.applyGoogleProviderResult(result);
    },
    applyGoogleProviderResult(result: GoogleIdentityInitializeResult): boolean {
      this.isInitialized = true;
      this.isLoading = false;

      if (result.status === 'ready') {
        this.providerSetupState = 'ready';
        this.providerError = null;
        return true;
      }

      if (result.status === 'missing-client-id') {
        this.providerSetupState = 'missing-client-id';
        this.providerError =
          'Set VITE_GOOGLE_CLIENT_ID in frontend/.env.local before using Google login in oidc mode.';
        return false;
      }

      this.providerSetupState = result.status === 'unsupported' ? 'error' : 'error';
      this.providerError =
        result.error ??
        'Google sign-in is unavailable until Google Identity Services finishes loading.';
      return false;
    },
    async startGoogleLogin(): Promise<boolean> {
      if (!this.isAuthRequiredMode || !this.isGoogleProvider) {
        return false;
      }

      if (this.providerSetupState !== 'ready') {
        this.isLoading = true;
        const isReady = await this.initializeGoogleProvider();

        if (!isReady) {
          return false;
        }
      }

      const didPrompt = googleIdentityProvider.prompt();

      if (!didPrompt) {
        this.providerSetupState = 'error';
        this.providerError = 'Google sign-in could not be started. Reload after the Google script finishes loading.';
      }

      return didPrompt;
    },
    handleGoogleCredentialResponse(response: GoogleCredentialResponse) {
      if (!response.credential) {
        this.providerError = 'Google sign-in returned no credential token.';
        this.isLoading = false;
        return;
      }

      this.setAuthenticatedSession({
        token: response.credential,
        user: extractUserFromCredentialToken(response.credential),
      });
    },
    handleUnauthorized() {
      this.clearSession();
    },
    logout() {
      if (this.isGoogleProvider) {
        googleIdentityProvider.logout();
      }

      this.clearSession();
      this.isInitialized = true;
      this.isLoading = false;
    },
  },
});
