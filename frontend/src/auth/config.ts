export type AuthMode = 'local-demo' | 'oidc';
export type AuthProvider = 'local-demo' | 'google' | 'oidc';

const defaultAuthMode: AuthMode = 'local-demo';

function normalizePublicValue(value: string | undefined): string | null {
  if (!value) {
    return null;
  }

  const trimmedValue = value.trim();
  return trimmedValue ? trimmedValue : null;
}

export function normalizeAuthMode(value: string | undefined): AuthMode {
  if (value === 'oidc') {
    return 'oidc';
  }

  return defaultAuthMode;
}

export function normalizeAuthProvider(
  value: string | undefined,
  mode: AuthMode = defaultAuthMode,
): AuthProvider {
  if (value === 'google' || value === 'oidc' || value === 'local-demo') {
    return value;
  }

  return mode === 'local-demo' ? 'local-demo' : 'oidc';
}

export const AUTH_MODE = normalizeAuthMode(import.meta.env.VITE_AUTH_MODE);
export const AUTH_PROVIDER = normalizeAuthProvider(import.meta.env.VITE_AUTH_PROVIDER, AUTH_MODE);
export const GOOGLE_CLIENT_ID = normalizePublicValue(import.meta.env.VITE_GOOGLE_CLIENT_ID);
