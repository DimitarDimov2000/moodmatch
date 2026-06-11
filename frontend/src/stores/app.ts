import type { RouteLocationRaw } from 'vue-router';

import { defineStore } from 'pinia';

export interface NavigationItem {
  label: string;
  to: RouteLocationRaw;
}

interface AppState {
  appTitle: string;
  navigationItems: NavigationItem[];
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    appTitle: 'MoodMatch',
    navigationItems: [
      {
        label: 'Dashboard',
        to: { name: 'dashboard' },
      },
    ],
  }),
  getters: {
    bootstrapSummary: () =>
      'Frontend scaffold is ready for route-level screens, typed API modules, and shared UI components.',
  },
});
