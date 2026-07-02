import { flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { googleIdentityProvider } from '@/auth/google-identity';
import { useAuthStore } from '@/stores/auth';

function createMockJwt(payload: Record<string, unknown>) {
  const encode = (value: object) =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');

  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(payload)}.signature`;
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('starts with a safe logged-out default state', () => {
    const authStore = useAuthStore();

    expect(authStore.mode).toBe('local-demo');
    expect(authStore.provider).toBe('local-demo');
    expect(authStore.token).toBeNull();
    expect(authStore.user).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
    expect(authStore.isInitialized).toBe(false);
    expect(authStore.isLoading).toBe(false);
    expect(authStore.canAccessProtectedRoutes).toBe(true);
    expect(authStore.providerSetupState).toBe('idle');
  });

  it('stores provider session details in memory and clears them on logout', () => {
    const authStore = useAuthStore();
    authStore.setAuthMode('oidc', 'google');
    authStore.setAuthenticatedSession({
      token: 'header.payload.signature',
      user: {
        displayName: 'Melli Example',
        email: 'melli@example.com',
      },
    });

    expect(authStore.isAuthenticated).toBe(true);
    expect(authStore.userDisplayName).toBe('Melli Example');
    expect(authStore.canAccessProtectedRoutes).toBe(true);

    authStore.logout();

    expect(authStore.isAuthenticated).toBe(false);
    expect(authStore.token).toBeNull();
    expect(authStore.user).toBeNull();
    expect(authStore.canAccessProtectedRoutes).toBe(false);
  });

  it('marks Google login as setup-needed when oidc mode has no client id', async () => {
    const authStore = useAuthStore();
    authStore.setAuthMode('oidc', 'google');
    authStore.setGoogleClientId(null);

    authStore.initialize();
    await flushPromises();

    expect(authStore.providerSetupState).toBe('missing-client-id');
    expect(authStore.providerError).toContain('VITE_GOOGLE_CLIENT_ID');
  });

  it('can use a mocked Google provider boundary to start login', async () => {
    const initializeSpy = vi.spyOn(googleIdentityProvider, 'initialize').mockResolvedValue({
      status: 'ready',
    });
    const promptSpy = vi.spyOn(googleIdentityProvider, 'prompt').mockReturnValue(true);

    const authStore = useAuthStore();
    authStore.setAuthMode('oidc', 'google');
    authStore.setGoogleClientId('google-client-id');

    authStore.initialize();
    await flushPromises();
    const didStart = await authStore.startGoogleLogin();

    expect(initializeSpy).toHaveBeenCalled();
    expect(promptSpy).toHaveBeenCalled();
    expect(didStart).toBe(true);
  });

  it('accepts a mocked Google credential response and decodes display data safely', () => {
    const authStore = useAuthStore();
    authStore.setAuthMode('oidc', 'google');

    authStore.handleGoogleCredentialResponse({
      credential: createMockJwt({
        name: 'Melli Example',
        email: 'melli@example.com',
        picture: 'https://example.com/avatar.png',
      }),
    });

    expect(authStore.isAuthenticated).toBe(true);
    expect(authStore.token).toContain('.signature');
    expect(authStore.user).toEqual({
      displayName: 'Melli Example',
      email: 'melli@example.com',
      avatarUrl: 'https://example.com/avatar.png',
    });
  });
});
