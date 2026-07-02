export type AuthMode = 'local-demo' | 'local-password';
export type AuthProvider = 'local-demo' | 'local-password';

const defaultAuthMode: AuthMode = 'local-password';

export function normalizeAuthMode(value: string | undefined): AuthMode {
  if (value === 'local-demo' || value === 'local-password') {
    return value;
  }

  return defaultAuthMode;
}

export function normalizeAuthProvider(
  value: string | undefined,
  mode: AuthMode = defaultAuthMode,
): AuthProvider {
  if (value === 'local-demo' || value === 'local-password') {
    return value;
  }

  return mode;
}

export const AUTH_MODE = normalizeAuthMode(import.meta.env.VITE_AUTH_MODE);
export const AUTH_PROVIDER = normalizeAuthProvider(import.meta.env.VITE_AUTH_PROVIDER, AUTH_MODE);
