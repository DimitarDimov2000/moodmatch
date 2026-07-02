import { createPinia } from 'pinia';
import { createApp } from 'vue';

import { configureApiClientAuth } from './api/client';
import App from './App.vue';
import { createAppRouter } from './router';
import { useAuthStore } from './stores/auth';
import './assets/styles/tokens.css';
import './assets/styles/base.css';

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

authStore.initialize();

app.use(pinia);
app.use(router);
app.mount('#app');
