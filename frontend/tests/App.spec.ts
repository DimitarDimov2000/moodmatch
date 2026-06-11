import { createPinia } from 'pinia';
import { mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';

import App from '@/App.vue';
import CandidatesView from '@/views/CandidatesView.vue';
import DashboardView from '@/views/DashboardView.vue';
import MediaCreateView from '@/views/MediaCreateView.vue';
import MediaDetailView from '@/views/MediaDetailView.vue';
import MediaLibraryView from '@/views/MediaLibraryView.vue';
import MatchesView from '@/views/MatchesView.vue';
import NotFoundView from '@/views/NotFoundView.vue';
import ProfileView from '@/views/ProfileView.vue';

async function mountApp() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: DashboardView },
      { path: '/profile', name: 'profile', component: ProfileView },
      { path: '/media', name: 'media-list', component: MediaLibraryView },
      { path: '/media/new', name: 'media-create', component: MediaCreateView },
      { path: '/media/:id', name: 'media-detail', component: MediaDetailView },
      { path: '/candidates', name: 'candidates', component: CandidatesView },
      { path: '/matches', name: 'matches', component: MatchesView },
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
    expect(wrapper.text()).toContain('Profil');
    expect(wrapper.text()).toContain('Medien');
    expect(wrapper.text()).toContain('Kandidaten');
    expect(wrapper.text()).toContain('Matches');
    expect(wrapper.text()).toContain('Minimal, buildable MoodMatch shell');
  });
});
