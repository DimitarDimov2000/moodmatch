import { createPinia } from 'pinia';
import { createApp } from 'vue';

import { configureApiClientAuth } from './api/client';
import App from './App.vue';
import { i18n, initializeI18n } from './i18n';
import { createAppRouter } from './router';
import { initializeTheme } from './composables/useTheme';
import { useAuthStore } from './stores/auth';
import './assets/styles/tokens.css';
import './assets/styles/base.css';

async function bootstrap() {
  initializeTheme();
  initializeI18n();

  const app = createApp(App);
  const pinia = createPinia();
  const router = createAppRouter({ pinia });
  const authStore = useAuthStore(pinia);

  configureApiClientAuth({
    getAccessToken: () => authStore.token,
    onUnauthorized: (error) => {
      authStore.handleUnauthorized();

      if (!authStore.isAuthRequiredMode || router.currentRoute.value.name === 'login') {
        return;
      }

      void router.push({
        name: 'login',
        query: {
          reason: 'session-expired',
          redirect: router.currentRoute.value.fullPath,
          status: String(error.status),
        },
      });
    },
  });

  await authStore.initialize();

  app.use(pinia);
  app.use(i18n);
  app.use(router);
  await router.isReady();
  app.mount('#app');
}

void bootstrap();
