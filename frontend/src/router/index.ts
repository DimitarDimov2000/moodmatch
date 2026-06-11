import { createRouter, createWebHistory } from 'vue-router';

import DashboardView from '@/views/DashboardView.vue';
import CandidatesView from '@/views/CandidatesView.vue';
import MediaCreateView from '@/views/MediaCreateView.vue';
import MediaDetailView from '@/views/MediaDetailView.vue';
import MediaLibraryView from '@/views/MediaLibraryView.vue';
import MatchesView from '@/views/MatchesView.vue';
import NotFoundView from '@/views/NotFoundView.vue';
import ProfileView from '@/views/ProfileView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
      meta: {
        title: 'Dashboard',
      },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: {
        title: 'Profil',
      },
    },
    {
      path: '/media',
      name: 'media-list',
      component: MediaLibraryView,
      meta: {
        title: 'Media Library',
      },
    },
    {
      path: '/media/new',
      name: 'media-create',
      component: MediaCreateView,
      meta: {
        title: 'Neues Medium',
      },
    },
    {
      path: '/media/:id',
      name: 'media-detail',
      component: MediaDetailView,
      meta: {
        title: 'Medium Details',
      },
    },
    {
      path: '/candidates',
      name: 'candidates',
      component: CandidatesView,
      meta: {
        title: 'Kandidaten',
      },
    },
    {
      path: '/matches',
      name: 'matches',
      component: MatchesView,
      meta: {
        title: 'Matches',
      },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
      meta: {
        title: 'Not Found',
      },
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

router.afterEach((to) => {
  const suffix = typeof to.meta.title === 'string' ? ` | ${to.meta.title}` : '';
  document.title = `MoodMatch${suffix}`;
});

export default router;
