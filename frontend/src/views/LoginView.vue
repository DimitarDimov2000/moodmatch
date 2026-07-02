<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';

import AppMessage from '@/components/common/AppMessage.vue';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const authStore = useAuthStore();
const { isAuthenticated, isLoading, userDisplayName } = storeToRefs(authStore);

const redirectTarget = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' && redirect ? redirect : '/';
});

const loginTitle = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'Local demo mode is active.';
  }

  if (route.query.reason === 'session-expired') {
    return 'Your session expired or was rejected by the backend.';
  }

  if (isAuthenticated.value) {
    return 'You are already authenticated.';
  }

  return 'Sign-in is required for protected routes.';
});

const loginDescription = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'Local development keeps private routes accessible without a real identity provider so the existing workflow continues to work.';
  }

  if (isAuthenticated.value) {
    return 'The provider-backed login callback can send you back to your protected routes from here once Google/OIDC is connected.';
  }

  return 'This frontend now has auth state, route protection, bearer-token support, and a dedicated login route. Real Google/OIDC button wiring is the next step and still needs provider setup.';
});

const providerHint = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'No provider setup is required in local-demo mode.';
  }

  if (authStore.provider === 'google' && authStore.hasGoogleClientIdConfigured) {
    return 'A public Google client ID is configured, but the Google Identity Services client is intentionally not wired in yet.';
  }

  if (authStore.provider === 'google') {
    return 'Set VITE_GOOGLE_CLIENT_ID only after the real Google Identity Services integration is added.';
  }

  return 'This placeholder stays provider-neutral so a future OIDC client can plug into the auth store without changing the protected-route flow.';
});
</script>

<template>
  <section class="login-view page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Login
        </p>
        <h1 class="page-title">
          Provider-ready authentication entry
        </h1>
        <p class="page-copy">
          This phase prepares the frontend for a future deployed bearer-token login flow without shipping a fake production sign-in.
        </p>
      </div>
    </header>

    <AppMessage
      v-if="isLoading"
      title="Authentication state is loading"
      description="Frontend auth initialization is being prepared."
      tone="info"
    />

    <template v-else>
      <AppMessage
        :title="loginTitle"
        :description="loginDescription"
        :tone="authStore.mode === 'local-demo' ? 'info' : 'warning'"
      />

      <section class="login-view__cards">
        <article class="page-card login-view__card">
          <p class="eyebrow">
            Current mode
          </p>
          <h2>{{ authStore.mode }}</h2>
          <p class="body-muted">
            Provider: {{ authStore.providerLabel }}
          </p>
        </article>

        <article class="page-card login-view__card">
          <p class="eyebrow">
            Current user
          </p>
          <h2>{{ userDisplayName ?? 'Signed out' }}</h2>
          <p class="body-muted">
            {{ isAuthenticated ? 'A bearer token is available in memory.' : 'No token is stored yet.' }}
          </p>
        </article>

        <article class="page-card login-view__card">
          <p class="eyebrow">
            Next integration step
          </p>
          <h2>{{ authStore.providerLabel }}</h2>
          <p class="body-muted">
            {{ providerHint }}
          </p>
        </article>
      </section>

      <section class="page-card login-view__panel">
        <h2 class="section-title">
          Login UI foundation
        </h2>
        <p class="page-copy">
          A real provider callback should eventually call the auth store with the verified frontend token and user display information.
        </p>

        <div class="login-view__actions">
          <button
            class="button button--primary"
            type="button"
            disabled
          >
            {{ authStore.provider === 'google' ? 'Google login coming next' : 'OIDC login coming next' }}
          </button>

          <RouterLink
            v-if="authStore.mode === 'local-demo' || isAuthenticated"
            class="button button--secondary"
            :to="redirectTarget"
          >
            {{ authStore.mode === 'local-demo' ? 'Continue to the app' : 'Return to the app' }}
          </RouterLink>

          <button
            v-else
            class="button button--secondary"
            type="button"
            disabled
          >
            Protected routes unlock after provider setup
          </button>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.login-view__cards {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.login-view__card,
.login-view__panel {
  padding: 1.25rem;
}

.login-view__card h2,
.login-view__card p,
.login-view__panel h2,
.login-view__panel p {
  margin-top: 0.5rem;
  margin-bottom: 0;
}

.login-view__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

@media (max-width: 900px) {
  .login-view__cards {
    grid-template-columns: 1fr;
  }
}
</style>
