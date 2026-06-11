import { createPinia } from 'pinia';
import { mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';

import App from '@/App.vue';
import DashboardView from '@/views/DashboardView.vue';
import NotFoundView from '@/views/NotFoundView.vue';

async function mountApp() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: DashboardView },
      { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
    ],
  });

  await router.push('/');
  await router.isReady();

  return mount(App, {
    global: {
      plugins: [createPinia(), router],
    },
  });
}

describe('App', () => {
  it('renders the application shell and dashboard scaffold', async () => {
    const wrapper = await mountApp();

    expect(wrapper.text()).toContain('MoodMatch');
    expect(wrapper.text()).toContain('Minimal, buildable MoodMatch shell');
  });
});
