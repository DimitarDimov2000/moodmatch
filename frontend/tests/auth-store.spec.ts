import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { login, logout, register } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/api/auth', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
  register: vi.fn(),
}));

const loginMock = vi.mocked(login);
const logoutMock = vi.mocked(logout);
const registerMock = vi.mocked(register);

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  afterEach(() => {
    vi.clearAllMocks();
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
  });
});
