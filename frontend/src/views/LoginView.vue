<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';

import AppMessage from '@/components/common/AppMessage.vue';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const authStore = useAuthStore();
const { isAuthenticated, isLoading, providerError, userDisplayName } = storeToRefs(authStore);

const redirectTarget = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' && redirect ? redirect : '/';
});

const isGoogleOidcMode = computed(() => authStore.isAuthRequiredMode && authStore.isGoogleProvider);

const loginTitle = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'Local demo mode is active.';
  }

  if (authStore.needsGoogleClientId) {
    return 'Google login setup is still required.';
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

  if (authStore.needsGoogleClientId) {
    return 'This frontend expects Google Identity Services in oidc mode, but no public Google client ID is configured yet.';
  }

  if (isAuthenticated.value) {
    return 'Your Google credential token is stored in memory and can already be sent to the backend as a bearer token.';
  }

  if (authStore.hasGoogleLoginOption) {
    return 'Google Identity Services is configured for this frontend. Start sign-in here to request a credential token for protected API calls.';
  }

  return 'This frontend keeps a provider boundary in place so the auth store and protected routes do not depend on raw Google window globals.';
});

const providerHint = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'No provider setup is required in local-demo mode.';
  }

  if (authStore.needsGoogleClientId) {
    return 'Add a public VITE_GOOGLE_CLIENT_ID value in frontend/.env.local and keep real Google secrets out of the frontend.';
  }

  if (authStore.hasGoogleLoginOption) {
    return 'The provider module lazily loads Google Identity Services and passes the credential response into the auth store.';
  }

  return 'This build currently ships a Google-specific provider boundary for oidc mode.';
});

const primaryActionLabel = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'Local demo mode active';
  }

  if (isAuthenticated.value) {
    return 'Google session received';
  }

  if (authStore.needsGoogleClientId) {
    return 'Google client ID required';
  }

  if (isGoogleOidcMode.value && isLoading.value) {
    return 'Preparing Google sign-in...';
  }

  if (isGoogleOidcMode.value) {
    return 'Continue with Google';
  }

  return 'OIDC provider unavailable';
});

const primaryActionDisabled = computed(() => {
  if (!isGoogleOidcMode.value) {
    return true;
  }

  return authStore.needsGoogleClientId || isLoading.value || isAuthenticated.value;
});

async function handlePrimaryAction() {
  if (primaryActionDisabled.value) {
    return;
  }

  await authStore.startGoogleLogin();
}
</script>

<template>
  <section class="login-view page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Login
        </p>
        <h1 class="page-title">
          Google-ready authentication entry
        </h1>
        <p class="page-copy">
          This phase connects the existing auth foundation to a real Google Identity Services boundary without requiring live credentials for local demo work or tests.
        </p>
      </div>
    </header>

    <AppMessage
      v-if="isLoading"
      title="Authentication state is loading"
      description="Frontend provider initialization is preparing the current login state."
      tone="info"
    />

    <AppMessage
      :title="loginTitle"
      :description="loginDescription"
      :tone="authStore.mode === 'local-demo' ? 'info' : authStore.needsGoogleClientId ? 'warning' : 'info'"
    />

    <AppMessage
      v-if="providerError"
      title="Provider setup detail"
      :description="providerError"
      tone="warning"
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
          Provider boundary
        </p>
        <h2>{{ authStore.providerLabel }}</h2>
        <p class="body-muted">
          {{ providerHint }}
        </p>
      </article>
    </section>

    <section class="page-card login-view__panel">
      <h2 class="section-title">
        Login actions
      </h2>
      <p class="page-copy">
        The frontend stores the Google credential token in memory only and sends it to the backend through the existing bearer-token API client when available.
      </p>

      <div class="login-view__actions">
        <button
          class="button button--primary"
          type="button"
          :disabled="primaryActionDisabled"
          @click="handlePrimaryAction"
        >
          {{ primaryActionLabel }}
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
          Protected routes unlock after login
        </button>
      </div>
    </section>
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
