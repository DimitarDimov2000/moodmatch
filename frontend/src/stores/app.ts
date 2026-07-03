import type { RouteLocationRaw } from 'vue-router';

import { defineStore } from 'pinia';

export interface NavigationItem {
  labelKey: string;
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
        labelKey: 'navigation.dashboard',
        to: { name: 'dashboard' },
      },
      {
        labelKey: 'navigation.profile',
        to: { name: 'profile' },
      },
      {
        labelKey: 'navigation.search',
        to: { name: 'external-search' },
      },
      {
        labelKey: 'navigation.media',
        to: { name: 'media-list' },
      },
      {
        labelKey: 'navigation.candidates',
        to: { name: 'candidates' },
      },
      {
        labelKey: 'navigation.swipe',
        to: { name: 'swipe' },
      },
      {
        labelKey: 'navigation.matches',
        to: { name: 'matches' },
      },
    ],
  }),
  getters: {
    bootstrapSummary: () => 'MoodMatch',
  },
});
