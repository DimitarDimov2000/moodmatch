import { createRouter, createWebHistory } from 'vue-router';

import DashboardView from '@/views/DashboardView.vue';
import MediaCreateView from '@/views/MediaCreateView.vue';
import MediaDetailView from '@/views/MediaDetailView.vue';
import MediaLibraryView from '@/views/MediaLibraryView.vue';
import NotFoundView from '@/views/NotFoundView.vue';

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
