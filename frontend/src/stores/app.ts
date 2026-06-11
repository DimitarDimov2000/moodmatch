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
        label: 'Medien',
        to: { name: 'media-list' },
      },
      {
        label: 'Neu',
        to: { name: 'media-create' },
      },
    ],
  }),
  getters: {
    bootstrapSummary: () =>
      'Die erste echte UI ist bereit fuer Medienverwaltung, Tag-Anzeige und typed API-basierte Formularfluesse.',
  },
});
