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
      {
        label: 'Profil',
        to: { name: 'profile' },
      },
      {
        label: 'Suche',
        to: { name: 'external-search' },
      },
      {
        label: 'Medien',
        to: { name: 'media-list' },
      },
      {
        label: 'Kandidaten',
        to: { name: 'candidates' },
      },
      {
        label: 'Swipe',
        to: { name: 'swipe' },
      },
      {
        label: 'Matches',
        to: { name: 'matches' },
      },
    ],
  }),
  getters: {
    bootstrapSummary: () =>
      'MoodMatch verbindet bereits Profil, Kandidaten, Matches, Swipe-Entscheidungen und typed API-basierte Medienfluesse in einer lauffaehigen lokalen Anwendung.',
  },
});
