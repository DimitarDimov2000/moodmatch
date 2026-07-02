import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser, login, logout, register } from '@/api/auth';
import { ApiRequestError } from '@/api/client';
import { AUTH_SESSION_STORAGE_KEY, useAuthStore } from '@/stores/auth';

vi.mock('@/api/auth', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
  register: vi.fn(),
}));

const getCurrentUserMock = vi.mocked(getCurrentUser);
const loginMock = vi.mocked(login);
const logoutMock = vi.mocked(logout);
const registerMock = vi.mocked(register);

function readStoredSession() {
  const rawValue = window.localStorage.getItem(AUTH_SESSION_STORAGE_KEY);
  return rawValue ? JSON.parse(rawValue) : null;
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
  });

  it('starts with a safe logged-out local-password default state', () => {
    const authStore = useAuthStore();

    expect(authStore.mode).toBe('local-password');
    expect(authStore.provider).toBe('local-password');
    expect(authStore.token).toBeNull();
    expect(authStore.user).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
    expect(authStore.canAccessProtectedRoutes).toBe(false);
  });

  it('keeps private routes accessible in local-demo mode', () => {
    const authStore = useAuthStore();
    authStore.setAuthMode('local-demo');

    expect(authStore.canAccessProtectedRoutes).toBe(true);
    expect(authStore.providerLabel).toBe('Local Demo');
  });

  it('stores a successful login token and safe user details', async () => {
    loginMock.mockResolvedValue({
      token: 'local-token',
      user: {
        id: 'user-id',
        email: 'melli@example.com',
        displayName: 'Melli Example',
      },
    });

    const authStore = useAuthStore();
    const didLogin = await authStore.loginWithPassword('melli@example.com', 'password123');

    expect(didLogin).toBe(true);
    expect(loginMock).toHaveBeenCalledWith({
      email: 'melli@example.com',
      password: 'password123',
    });
    expect(authStore.isAuthenticated).toBe(true);
    expect(authStore.token).toBe('local-token');
    expect(authStore.userDisplayName).toBe('Melli Example');
    expect(authStore.canAccessProtectedRoutes).toBe(true);
    expect(readStoredSession()).toEqual({
      token: 'local-token',
      user: {
        id: 'user-id',
        email: 'melli@example.com',
        displayName: 'Melli Example',
      },
    });
  });

  it('registers a new account and trims optional display name', async () => {
    registerMock.mockResolvedValue({
      token: 'register-token',
      user: {
        id: 'user-id',
        email: 'student@example.com',
        displayName: 'Student',
      },
    });

    const authStore = useAuthStore();
    const didRegister = await authStore.registerWithPassword(
      'student@example.com',
      'password123',
      ' Student ',
    );

    expect(didRegister).toBe(true);
    expect(registerMock).toHaveBeenCalledWith({
      email: 'student@example.com',
      password: 'password123',
      displayName: 'Student',
    });
    expect(authStore.token).toBe('register-token');
    expect(authStore.user?.email).toBe('student@example.com');
    expect(readStoredSession()).toEqual({
      token: 'register-token',
      user: {
        id: 'user-id',
        email: 'student@example.com',
        displayName: 'Student',
      },
    });
  });

  it('restores a stored session and verifies it through /auth/me', async () => {
    window.localStorage.setItem(
      AUTH_SESSION_STORAGE_KEY,
      JSON.stringify({
        token: 'restored-token',
        user: {
          id: 'stored-id',
          email: 'stored@example.com',
          displayName: 'Stored User',
        },
      }),
    );
    getCurrentUserMock.mockResolvedValue({
      id: 'verified-id',
      email: 'verified@example.com',
      displayName: 'Verified User',
    });

    const authStore = useAuthStore();
    await authStore.initialize();

    expect(getCurrentUserMock).toHaveBeenCalledTimes(1);
    expect(authStore.isInitialized).toBe(true);
    expect(authStore.isAuthenticated).toBe(true);
    expect(authStore.token).toBe('restored-token');
    expect(authStore.user).toEqual({
      id: 'verified-id',
      email: 'verified@example.com',
      displayName: 'Verified User',
    });
    expect(readStoredSession()).toEqual({
      token: 'restored-token',
      user: {
        id: 'verified-id',
        email: 'verified@example.com',
        displayName: 'Verified User',
      },
    });
  });

  it('clears an invalid stored session when /auth/me rejects it', async () => {
    window.localStorage.setItem(
      AUTH_SESSION_STORAGE_KEY,
      JSON.stringify({
        token: 'expired-token',
        user: {
          id: 'stored-id',
          email: 'stored@example.com',
          displayName: 'Stored User',
        },
      }),
    );
    getCurrentUserMock.mockRejectedValue(
      new ApiRequestError('Unauthorized', {
        status: 401,
        code: 'UNAUTHORIZED',
      }),
    );

    const authStore = useAuthStore();
    await authStore.initialize();

    expect(authStore.isInitialized).toBe(true);
    expect(authStore.isAuthenticated).toBe(false);
    expect(authStore.token).toBeNull();
    expect(authStore.user).toBeNull();
    expect(window.localStorage.getItem(AUTH_SESSION_STORAGE_KEY)).toBeNull();
  });

  it('clears the session on logout', async () => {
    const authStore = useAuthStore();
    authStore.setAuthenticatedSession({
      token: 'local-token',
      user: {
        id: 'user-id',
        email: 'melli@example.com',
      },
    });

    await authStore.logout();

    expect(logoutMock).toHaveBeenCalledTimes(1);
    expect(authStore.isAuthenticated).toBe(false);
    expect(authStore.token).toBeNull();
    expect(authStore.user).toBeNull();
    expect(window.localStorage.getItem(AUTH_SESSION_STORAGE_KEY)).toBeNull();
  });
});
