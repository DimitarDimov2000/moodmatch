import { createPinia } from 'pinia';
import { flushPromises } from '@vue/test-utils';
import { createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi } from 'vitest';

import { createAppRouter } from '@/router';
import { useAuthStore } from '@/stores/auth';

describe('router auth protection', () => {
  it('redirects unauthenticated users to login in local-password mode', async () => {
    const pinia = createPinia();
    const authStore = useAuthStore(pinia);
    authStore.setAuthMode('local-password');

    const router = createAppRouter({
      history: createMemoryHistory(),
      pinia,
    });

    await router.push('/matches');

    expect(router.currentRoute.value.name).toBe('login');
    expect(router.currentRoute.value.query.reason).toBe('login-required');
    expect(router.currentRoute.value.query.redirect).toBe('/matches');
  });

  it('keeps private routes accessible in local-demo mode', async () => {
    const pinia = createPinia();
    const authStore = useAuthStore(pinia);
    authStore.setAuthMode('local-demo');

    const router = createAppRouter({
      history: createMemoryHistory(),
      pinia,
    });

    await router.push('/profile');

    expect(router.currentRoute.value.name).toBe('profile');
  });

  it('keeps not-found behavior working in auth-required mode', async () => {
    const pinia = createPinia();
    const authStore = useAuthStore(pinia);
    authStore.setAuthMode('local-password');

    const router = createAppRouter({
      history: createMemoryHistory(),
      pinia,
    });

    await router.push('/definitely-missing');

    expect(router.currentRoute.value.name).toBe('not-found');
  });

  it('does not redirect to login before auth restore finishes', async () => {
    const pinia = createPinia();
    const authStore = useAuthStore(pinia);
    authStore.setAuthMode('local-password');

    let resolveInitialization!: () => void;
    vi.spyOn(authStore, 'initialize').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveInitialization = () => {
            authStore.setAuthenticatedSession(
              {
                token: 'restored-token',
                user: {
                  id: 'user-id',
                  email: 'melli@example.com',
                },
              },
              false,
            );
            resolve();
          };
        }),
    );

    const router = createAppRouter({
      history: createMemoryHistory(),
      pinia,
    });

    const navigationPromise = router.push('/matches');
    await flushPromises();

    expect(router.currentRoute.value.name).not.toBe('login');

    resolveInitialization();
    await navigationPromise;

    expect(router.currentRoute.value.name).toBe('matches');
  });
});
