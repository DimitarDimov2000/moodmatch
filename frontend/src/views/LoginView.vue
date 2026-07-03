<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import BrandMark from '@/components/brand/BrandMark.vue';
import AppMessage from '@/components/common/AppMessage.vue';
import FormField from '@/components/common/FormField.vue';
import ThemePreferenceSwitch from '@/components/common/ThemePreferenceSwitch.vue';
import { useAuthStore } from '@/stores/auth';

type AuthPanel = 'login' | 'register';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { isAuthenticated, isLoading, providerError, userDisplayName } = storeToRefs(authStore);
const activePanel = ref<AuthPanel>('login');

const loginForm = reactive({
  email: '',
  password: '',
});

const registerForm = reactive({
  email: '',
  password: '',
  displayName: '',
});

const redirectTarget = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' && redirect ? redirect : '/';
});

const pageTitle = computed(() => {
  if (route.query.reason === 'session-expired') {
    return 'Log in to continue';
  }

  if (isAuthenticated.value) {
    return 'You are signed in';
  }

  return activePanel.value === 'register' ? 'Create your MoodMatch account' : 'Log in to continue';
});

async function handleLogin() {
  const didLogin = await authStore.loginWithPassword(loginForm.email, loginForm.password);
  if (didLogin) {
    await router.push(redirectTarget.value);
  }
}

async function handleRegister() {
  const didRegister = await authStore.registerWithPassword(
    registerForm.email,
    registerForm.password,
    registerForm.displayName,
  );
  if (didRegister) {
    await router.push(redirectTarget.value);
  }
}
</script>

<template>
  <section class="login-view page-stack">
    <header class="page-header login-view__header">
      <div class="login-view__intro">
        <div class="login-view__toolbar">
          <ThemePreferenceSwitch class="login-view__theme-switch" />
        </div>

        <div
          class="login-view__brand-lockup"
          data-testid="login-brand-lockup"
        >
          <BrandMark
            test-id="login-brand-mark"
            class="login-view__brand-mark"
          />
          <div class="login-view__brand-copy">
            <span class="login-view__brand-name">MoodMatch</span>
            <span class="login-view__brand-subtitle">Media Discovery App</span>
          </div>
        </div>

        <p class="eyebrow">
          Login
        </p>
        <h1 class="page-title">
          {{ pageTitle }}
        </h1>
        <p class="page-copy">
          Your media and matches are private to your account.
        </p>
      </div>
    </header>

    <AppMessage
      v-if="providerError"
      title="Authentication failed"
      :description="providerError"
      tone="warning"
    />

    <AppMessage
      v-if="isAuthenticated"
      title="Session active"
      :description="`Signed in as ${userDisplayName ?? 'your account'}.`"
      tone="info"
    />

    <section class="page-card login-view__panel">
      <div
        class="login-view__tabs"
        role="tablist"
        aria-label="Authentication mode"
      >
        <button
          class="login-view__tab"
          :class="{ 'login-view__tab--active': activePanel === 'login' }"
          type="button"
          role="tab"
          :aria-selected="activePanel === 'login'"
          @click="activePanel = 'login'"
        >
          Login
        </button>
        <button
          class="login-view__tab"
          :class="{ 'login-view__tab--active': activePanel === 'register' }"
          type="button"
          role="tab"
          :aria-selected="activePanel === 'register'"
          @click="activePanel = 'register'"
        >
          Create account
        </button>
      </div>

      <form
        v-if="activePanel === 'login'"
        class="login-view__form"
        @submit.prevent="handleLogin"
      >
        <FormField
          label="Email"
          required
        >
          <input
            v-model="loginForm.email"
            class="login-view__input"
            type="email"
            autocomplete="email"
            required
          >
        </FormField>

        <FormField
          label="Password"
          required
        >
          <input
            v-model="loginForm.password"
            class="login-view__input"
            type="password"
            autocomplete="current-password"
            minlength="8"
            required
          >
        </FormField>

        <button
          class="button button--primary login-view__submit"
          type="submit"
          :disabled="isLoading"
        >
          {{ isLoading ? 'Logging in...' : 'Log in' }}
        </button>
      </form>

      <form
        v-else
        class="login-view__form"
        @submit.prevent="handleRegister"
      >
        <FormField label="Display name">
          <input
            v-model="registerForm.displayName"
            class="login-view__input"
            type="text"
            autocomplete="name"
          >
        </FormField>

        <FormField
          label="Email"
          required
        >
          <input
            v-model="registerForm.email"
            class="login-view__input"
            type="email"
            autocomplete="email"
            required
          >
        </FormField>

        <FormField
          label="Password"
          hint="Use at least 8 characters."
          required
        >
          <input
            v-model="registerForm.password"
            class="login-view__input"
            type="password"
            autocomplete="new-password"
            minlength="8"
            required
          >
        </FormField>

        <button
          class="button button--primary login-view__submit"
          type="submit"
          :disabled="isLoading"
        >
          {{ isLoading ? 'Creating account...' : 'Create account' }}
        </button>
      </form>
    </section>
  </section>
</template>

<style scoped>
.login-view {
  position: relative;
  justify-items: center;
}

.login-view__header {
  position: relative;
  width: 100%;
  justify-content: center;
}

.login-view__intro {
  display: grid;
  gap: 0.85rem;
  width: min(100%, 38rem);
  justify-items: center;
  padding-top: clamp(0.4rem, 1vw, 0.7rem);
  text-align: center;
}

.login-view__toolbar {
  position: absolute;
  top: 0;
  right: 0;
}

.login-view__brand-lockup {
  display: inline-grid;
  gap: 0.55rem;
  min-width: 0;
  justify-items: center;
}

.login-view__brand-mark {
  width: clamp(5.6rem, 13vw, 7rem);
  height: clamp(5.6rem, 13vw, 7rem);
}

.login-view__brand-copy {
  display: grid;
  gap: 0.15rem;
}

.login-view__brand-name {
  color: var(--color-text-primary);
  font-size: clamp(1.55rem, 3.6vw, 2rem);
  font-weight: 800;
  letter-spacing: -0.04em;
}

.login-view__brand-subtitle {
  color: var(--color-text-secondary);
  font-size: 0.98rem;
  line-height: 1.2;
}

.login-view :deep(.page-title) {
  max-width: 12ch;
}

.login-view :deep(.page-copy) {
  max-width: 34rem;
  margin-top: 0;
}

.login-view__panel {
  display: grid;
  gap: 1.25rem;
  width: min(100%, 38rem);
  padding: clamp(1.1rem, 2.6vw, 1.4rem);
}

.login-view__tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  padding: 0.25rem;
  background: var(--theme-login-tabs-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.login-view__tab {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  font-weight: 700;
}

.login-view__tab--active {
  background: var(--theme-login-tab-active-background);
  color: var(--color-text-primary);
  box-shadow: var(--theme-login-tab-active-shadow);
}

.login-view__form {
  display: grid;
  gap: 1rem;
}

.login-view__input {
  background: var(--theme-input-background);
}

.login-view__input:focus {
  outline: 2px solid color-mix(in srgb, var(--color-accent) 35%, transparent);
  border-color: var(--color-accent);
}

.login-view__submit {
  justify-self: stretch;
}

@media (min-width: 640px) {
  .login-view__submit {
    width: auto;
    justify-self: start;
  }
}

@media (max-width: 560px) {
  .login-view__toolbar {
    top: -0.1rem;
    right: 0;
  }

  .login-view__brand-subtitle {
    font-size: 0.9rem;
  }
}
</style>
