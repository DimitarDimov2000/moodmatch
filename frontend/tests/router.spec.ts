import { createPinia } from 'pinia';
import { createMemoryHistory } from 'vue-router';
import { describe, expect, it } from 'vitest';

import { createAppRouter } from '@/router';
import { useAuthStore } from '@/stores/auth';

describe('router auth protection', () => {
  it('redirects unauthenticated users to login in oidc mode', async () => {
    const pinia = createPinia();
    const authStore = useAuthStore(pinia);
    authStore.setAuthMode('oidc', 'google');

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
    authStore.setAuthMode('oidc', 'oidc');

    const router = createAppRouter({
      history: createMemoryHistory(),
      pinia,
    });

    await router.push('/definitely-missing');

    expect(router.currentRoute.value.name).toBe('not-found');
  });
});
