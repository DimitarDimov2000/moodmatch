import type { Pinia } from 'pinia';
import { watch } from 'vue';
import type { RouterHistory } from 'vue-router';
import { createRouter, createWebHistory } from 'vue-router';

import { i18n } from '@/i18n';
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
    titleKey?: string;
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
          titleKey: 'routes.dashboard.title',
          requiresAuth: true,
        },
      },
      {
        path: '/login',
        name: 'login',
        component: LoginView,
        meta: {
          titleKey: 'routes.login.title',
        },
      },
      {
        path: '/profile',
        name: 'profile',
        component: ProfileView,
        meta: {
          titleKey: 'routes.profile.title',
          requiresAuth: true,
        },
      },
      {
        path: '/external-search',
        name: 'external-search',
        component: ExternalSearchView,
        meta: {
          titleKey: 'routes.search.title',
          requiresAuth: true,
        },
      },
      {
        path: '/media',
        name: 'media-list',
        component: MediaLibraryView,
        meta: {
          titleKey: 'routes.media.title',
          requiresAuth: true,
        },
      },
      {
        path: '/media/new',
        name: 'media-create',
        component: MediaCreateView,
        meta: {
          titleKey: 'routes.mediaCreate.title',
          requiresAuth: true,
        },
      },
      {
        path: '/media/:id',
        name: 'media-detail',
        component: MediaDetailView,
        meta: {
          titleKey: 'routes.mediaDetail.title',
          requiresAuth: true,
        },
      },
      {
        path: '/candidates',
        name: 'candidates',
        component: CandidatesView,
        meta: {
          titleKey: 'routes.candidates.title',
          requiresAuth: true,
        },
      },
      {
        path: '/matches',
        name: 'matches',
        component: MatchesView,
        meta: {
          titleKey: 'routes.matches.title',
          requiresAuth: true,
        },
      },
      {
        path: '/swipe',
        name: 'swipe',
        component: SwipeView,
        meta: {
          titleKey: 'routes.swipe.title',
          requiresAuth: true,
        },
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: NotFoundView,
        meta: {
          titleKey: 'routes.notFound.title',
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

  const updateDocumentTitle = () => {
    const route = router.currentRoute.value;
    const appTitle = i18n.global.t('app.metadata.title');
    const suffix = route.meta.titleKey ? ` | ${i18n.global.t(route.meta.titleKey)}` : '';
    document.title = `${appTitle}${suffix}`;
  };

  router.afterEach(() => {
    updateDocumentTitle();
  });

  watch(() => i18n.global.locale.value, updateDocumentTitle);

  return router;
}
