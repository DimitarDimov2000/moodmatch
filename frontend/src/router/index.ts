import type { Pinia } from 'pinia';
import type { RouterHistory } from 'vue-router';
import { createRouter, createWebHistory } from 'vue-router';

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

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    title?: string;
  }
}

export interface CreateAppRouterOptions {
  history?: RouterHistory;
  pinia: Pinia;
}

export function createAppRouter({ history = createWebHistory(), pinia }: CreateAppRouterOptions) {
  const router = createRouter({
    history,
    routes: [
      {
        path: '/',
        name: 'dashboard',
        component: DashboardView,
        meta: {
          title: 'Dashboard',
          requiresAuth: true,
        },
      },
      {
        path: '/login',
        name: 'login',
        component: LoginView,
        meta: {
          title: 'Login',
        },
      },
      {
        path: '/profile',
        name: 'profile',
        component: ProfileView,
        meta: {
          title: 'Profil',
          requiresAuth: true,
        },
      },
      {
        path: '/external-search',
        name: 'external-search',
        component: ExternalSearchView,
        meta: {
          title: 'Externe Suche',
          requiresAuth: true,
        },
      },
      {
        path: '/media',
        name: 'media-list',
        component: MediaLibraryView,
        meta: {
          title: 'Mediathek',
          requiresAuth: true,
        },
      },
      {
        path: '/media/new',
        name: 'media-create',
        component: MediaCreateView,
        meta: {
          title: 'Neues Medium',
          requiresAuth: true,
        },
      },
      {
        path: '/media/:id',
        name: 'media-detail',
        component: MediaDetailView,
        meta: {
          title: 'Medien-Details',
          requiresAuth: true,
        },
      },
      {
        path: '/candidates',
        name: 'candidates',
        component: CandidatesView,
        meta: {
          title: 'Kandidaten',
          requiresAuth: true,
        },
      },
      {
        path: '/matches',
        name: 'matches',
        component: MatchesView,
        meta: {
          title: 'Matches',
          requiresAuth: true,
        },
      },
      {
        path: '/swipe',
        name: 'swipe',
        component: SwipeView,
        meta: {
          title: 'Swipe-Modus',
          requiresAuth: true,
        },
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: NotFoundView,
        meta: {
          title: 'Nicht gefunden',
        },
      },
    ],
    scrollBehavior() {
      return { top: 0 };
    },
  });

  router.beforeEach(async (to) => {
    const authStore = useAuthStore(pinia);
    await authStore.initialize();

    if (to.meta.requiresAuth && !authStore.canAccessProtectedRoutes) {
      return {
        name: 'login',
        query: {
          reason: 'login-required',
          redirect: to.fullPath,
        },
      };
    }

    return true;
  });

  router.afterEach((to) => {
    const suffix = typeof to.meta.title === 'string' ? ` | ${to.meta.title}` : '';
    document.title = `MoodMatch${suffix}`;
  });

  return router;
}
