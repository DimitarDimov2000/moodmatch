import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { beforeEach, vi } from 'vitest';

import App from '@/App.vue';
import { THEME_STORAGE_KEY } from '@/composables/useTheme';
import { useAuthStore } from '@/stores/auth';
import CandidatesView from '@/views/CandidatesView.vue';
import DashboardView from '@/views/DashboardView.vue';
import ExternalSearchView from '@/views/ExternalSearchView.vue';
import LoginView from '@/views/LoginView.vue';
import MatchesView from '@/views/MatchesView.vue';
import MediaCreateView from '@/views/MediaCreateView.vue';
import MediaDetailView from '@/views/MediaDetailView.vue';
import MediaLibraryView from '@/views/MediaLibraryView.vue';
import NotFoundView from '@/views/NotFoundView.vue';
import ProfileView from '@/views/ProfileView.vue';
import SwipeView from '@/views/SwipeView.vue';

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

async function mountApp(path = '/') {
  const pinia = createPinia();
  const authStore = useAuthStore(pinia);
  authStore.setAuthMode('local-demo');

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: DashboardView },
      { path: '/login', name: 'login', component: LoginView },
      { path: '/profile', name: 'profile', component: ProfileView },
      { path: '/external-search', name: 'external-search', component: ExternalSearchView },
      { path: '/media', name: 'media-list', component: MediaLibraryView },
      { path: '/media/new', name: 'media-create', component: MediaCreateView },
      { path: '/media/:id', name: 'media-detail', component: MediaDetailView },
      { path: '/candidates', name: 'candidates', component: CandidatesView },
      { path: '/matches', name: 'matches', component: MatchesView },
      { path: '/swipe', name: 'swipe', component: SwipeView },
      { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
    ],
  });

  await router.push(path);
  await router.isReady();

  return mount(App, {
    global: {
      plugins: [pinia, router],
    },
  });
}

describe('App', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.style.colorScheme = '';
  });

  it('renders the application shell and dashboard route', async () => {
    const wrapper = await mountApp();
    await flushPromises();

    expect(wrapper.text()).toContain('MoodMatch');
    expect(wrapper.find('[data-testid="brand-mark"]').exists()).toBe(true);
    expect(wrapper.get('[data-testid="brand-mark"]').attributes('src')).toContain('moodmatch_logo_dark.svg');
    expect(wrapper.text()).toContain('Profil');
    expect(wrapper.text()).toContain('Suche');
    expect(wrapper.text()).toContain('Medien');
    expect(wrapper.text()).toContain('Kandidaten');
    expect(wrapper.text()).toContain('Matches');
    expect(wrapper.text()).toContain('Swipe');
    expect(wrapper.text()).toContain('Dashboard');
  });

  it('renders authenticated navigation with an active route style', async () => {
    const wrapper = await mountApp('/matches');
    await flushPromises();

    const activeLinks = wrapper.findAll('.app-shell__nav-link.router-link-active');

    expect(wrapper.find('[data-testid="primary-nav-frame"]').exists()).toBe(true);
    expect(activeLinks.some((node) => node.text() === 'Matches')).toBe(true);
  });

  it('renders the login theme control without the global header and updates the root theme attribute', async () => {
    const wrapper = await mountApp('/login');
    await flushPromises();

    const themeSwitch = wrapper.get('[data-testid="theme-switch"]');

    expect(wrapper.find('[data-testid="brand-link"]').exists()).toBe(false);
    expect(themeSwitch.text()).toContain('Dark');
    expect(themeSwitch.attributes('aria-label')).toBe('Theme: Dark. Click to switch to Light.');
    expect(document.documentElement.dataset.theme).toBe('dark');

    await themeSwitch.trigger('click');

    expect(document.documentElement.dataset.theme).toBe('light');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    expect(themeSwitch.text()).toContain('Light');
  });
});
