import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';

import { useAuthStore } from '@/stores/auth';

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
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
});
