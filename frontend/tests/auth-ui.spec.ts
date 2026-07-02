import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory } from 'vue-router';
import { vi } from 'vitest';

import App from '@/App.vue';
import { createAppRouter } from '@/router';
import { useAuthStore } from '@/stores/auth';

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
  it('shows the login foundation when oidc mode blocks a protected route', async () => {
    const { router, wrapper } = await mountAppAtRoute('/matches', (authStore) => {
      authStore.setAuthMode('oidc', 'google');
    });

    expect(router.currentRoute.value.name).toBe('login');
    expect(wrapper.text()).toContain('Provider-ready authentication entry');
    expect(wrapper.text()).toContain('Google login coming next');
    expect(wrapper.text()).toContain('Signed out');
  });

  it('clears auth state and returns to login on logout', async () => {
    const { authStore, router, wrapper } = await mountAppAtRoute('/', (store) => {
      store.setAuthMode('oidc', 'google');
      store.setAuthenticatedSession({
        token: 'header.payload.signature',
        user: {
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

    expect(authStore.isAuthenticated).toBe(false);
    expect(router.currentRoute.value.name).toBe('login');
    expect(wrapper.text()).toContain('Signed out');
  });
});
