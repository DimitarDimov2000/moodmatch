import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory } from 'vue-router';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { login, logout, register } from '@/api/auth';
import App from '@/App.vue';
import { createAppRouter } from '@/router';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/api/auth', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn().mockResolvedValue(undefined),
  register: vi.fn(),
}));

vi.mock('@/api/media', () => ({
  listMedia: vi.fn().mockResolvedValue([]),
}));

vi.mock('@/api/profile', () => ({
  getProfile: vi.fn().mockResolvedValue({
    isReadyForMatching: false,
    profileRelevantMediaCount: 0,
    requiredProfileRelevantMediaCount: 3,
    explanationMessage: 'Noch zu wenig Daten.',
    contributingMedia: [],
    weightedTags: [],
  }),
}));

vi.mock('@/api/candidates', () => ({
  listCandidates: vi.fn().mockResolvedValue({
    candidates: [],
  }),
}));

vi.mock('@/api/matches', () => ({
  getMatches: vi.fn().mockResolvedValue({
    interestProfile: {
      isReadyForMatching: false,
      profileRelevantMediaCount: 0,
      requiredProfileRelevantMediaCount: 3,
      explanationMessage: 'Noch zu wenig Daten.',
      contributingMedia: [],
      weightedTags: [],
    },
    scoresSuppressed: true,
    explanationMessage: 'Noch keine Vergleichbarkeit.',
    scoringMethodNote: 'Relativ nur bei genug Daten.',
    matches: [],
  }),
}));

const loginMock = vi.mocked(login);
const logoutMock = vi.mocked(logout);
const registerMock = vi.mocked(register);

async function mountAppAtRoute(
  path: string,
  configureAuth?: (authStore: ReturnType<typeof useAuthStore>) => void,
) {
  const pinia = createPinia();
  const authStore = useAuthStore(pinia);

  configureAuth?.(authStore);

  const router = createAppRouter({
    history: createMemoryHistory(),
    pinia,
  });

  await router.push(path);
  await router.isReady();

  const wrapper = mount(App, {
    global: {
      plugins: [pinia, router],
    },
  });

  await flushPromises();

  return { authStore, router, wrapper };
}

describe('auth UI foundation', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('shows login and create-account forms without a Google button', async () => {
    const { wrapper } = await mountAppAtRoute('/login');

    expect(wrapper.find('[data-testid="login-brand-lockup"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="login-brand-mark"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="brand-link"]').exists()).toBe(false);
    expect(wrapper.text()).toContain('MoodMatch');
    expect(wrapper.text()).toContain('Media Discovery App');
    expect(wrapper.text()).toContain('Log in to continue');
    expect(wrapper.text()).toContain('Your media and matches are private to your account.');
    expect(wrapper.text()).toContain('Login');
    expect(wrapper.text()).toContain('Create account');
    expect(wrapper.text()).not.toContain('Continue with Google');
    expect(wrapper.find('[data-testid="google-signin-container"]').exists()).toBe(false);

    await wrapper
      .findAll('button')
      .find((candidate) => candidate.text() === 'Create account')
      ?.trigger('click');

    expect(wrapper.text()).toContain('Create your MoodMatch account');
    expect(wrapper.find('input[autocomplete="new-password"]').exists()).toBe(true);
  });

  it('redirects protected routes to local password login when signed out', async () => {
    const { router, wrapper } = await mountAppAtRoute('/matches');

    expect(router.currentRoute.value.name).toBe('login');
    expect(router.currentRoute.value.query.reason).toBe('login-required');
    expect(wrapper.text()).toContain('Log in to continue');
    expect(wrapper.find('[data-testid="brand-link"]').exists()).toBe(false);
  });

  it('stores a successful login and returns to the requested route', async () => {
    loginMock.mockResolvedValue({
      token: 'local-token',
      user: {
        id: 'user-id',
        email: 'melli@example.com',
        displayName: 'Melli Example',
      },
    });

    const { authStore, router, wrapper } = await mountAppAtRoute('/login?redirect=/matches');
    const inputs = wrapper.findAll('input');

    await inputs[0]?.setValue('melli@example.com');
    await inputs[1]?.setValue('password123');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(loginMock).toHaveBeenCalledWith({
      email: 'melli@example.com',
      password: 'password123',
    });
    expect(authStore.token).toBe('local-token');
    expect(router.currentRoute.value.name).toBe('matches');
  });

  it('registers an account from the create-account tab', async () => {
    registerMock.mockResolvedValue({
      token: 'register-token',
      user: {
        id: 'user-id',
        email: 'student@example.com',
        displayName: 'Student',
      },
    });

    const { authStore, wrapper } = await mountAppAtRoute('/login');

    await wrapper
      .findAll('button')
      .find((candidate) => candidate.text() === 'Create account')
      ?.trigger('click');

    const inputs = wrapper.findAll('input');
    await inputs[0]?.setValue('Student');
    await inputs[1]?.setValue('student@example.com');
    await inputs[2]?.setValue('password123');
    await wrapper.find('form').trigger('submit.prevent');
    await flushPromises();

    expect(registerMock).toHaveBeenCalledWith({
      email: 'student@example.com',
      password: 'password123',
      displayName: 'Student',
    });
    expect(authStore.token).toBe('register-token');
  });

  it('clears auth state and returns to login on logout', async () => {
    const { authStore, router, wrapper } = await mountAppAtRoute('/', (store) => {
      store.setAuthenticatedSession({
        token: 'local-token',
        user: {
          id: 'user-id',
          displayName: 'Melli Example',
          email: 'melli@example.com',
        },
      });
    });

    const logoutButton = wrapper
      .findAll('button')
      .find((candidate) => candidate.text() === 'Abmelden');

    expect(logoutButton).toBeDefined();

    await logoutButton?.trigger('click');
    await flushPromises();

    expect(logoutMock).toHaveBeenCalledTimes(1);
    expect(authStore.isAuthenticated).toBe(false);
    expect(router.currentRoute.value.name).toBe('login');
    expect(wrapper.find('[data-testid="brand-link"]').exists()).toBe(false);
  });
});
