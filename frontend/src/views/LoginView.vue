<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import BrandMark from "@/components/brand/BrandMark.vue";
import AppMessage from "@/components/common/AppMessage.vue";
import FormField from "@/components/common/FormField.vue";
import { i18n } from "@/i18n";
import LanguagePreferenceSwitch from "@/components/common/LanguagePreferenceSwitch.vue";
import ThemePreferenceSwitch from "@/components/common/ThemePreferenceSwitch.vue";
import { useAuthStore } from "@/stores/auth";

type AuthPanel = "login" | "register";

const route = useRoute();
const router = useRouter();
const { t } = i18n.global;
const authStore = useAuthStore();
const { isAuthenticated, isLoading, providerError, userDisplayName } =
  storeToRefs(authStore);
const activePanel = ref<AuthPanel>("login");

const loginForm = reactive({
  email: "",
  password: "",
});

const registerForm = reactive({
  email: "",
  password: "",
  displayName: "",
});

const redirectTarget = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === "string" && redirect ? redirect : "/";
});

const pageTitle = computed(() => {
  if (route.query.reason === "session-expired") {
    return t("login.titleLogin");
  }

  if (isAuthenticated.value) {
    return t("login.titleSignedIn");
  }

  return activePanel.value === "register"
    ? t("login.titleRegister")
    : t("login.titleLogin");
});

async function handleLogin() {
  const didLogin = await authStore.loginWithPassword(
    loginForm.email,
    loginForm.password,
  );
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
      <div class="login-view__toolbar">
        <ThemePreferenceSwitch class="login-view__theme-switch" />
        <LanguagePreferenceSwitch class="login-view__theme-switch" />
      </div>

      <div class="login-view__intro">
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
            <span class="login-view__brand-subtitle">{{ t("login.brandSubtitle") }}</span>
          </div>
        </div>

        <p class="eyebrow">
          {{ t("login.eyebrow") }}
        </p>
        <h1 class="page-title">
          {{ pageTitle }}
        </h1>
        <p class="page-copy">
          {{ t("login.intro") }}
        </p>
      </div>
    </header>

    <AppMessage
      v-if="providerError"
      :title="t('login.authFailed')"
      :description="providerError"
      tone="warning"
    />

    <AppMessage
      v-if="isAuthenticated"
      :title="t('login.sessionActive')"
      :description="t('login.signedInAs', { name: userDisplayName ?? t('common.states.account') })"
      tone="info"
    />

    <section class="page-card login-view__panel">
      <div class="login-view__panel-header">
        <div
          class="login-view__tabs"
          role="tablist"
          :aria-label="t('login.authMode')"
        >
          <button
            data-testid="auth-tab-login"
            class="login-view__tab"
            :class="{ 'login-view__tab--active': activePanel === 'login' }"
            type="button"
            role="tab"
            :aria-selected="activePanel === 'login'"
            @click="activePanel = 'login'"
          >
            {{ t("login.tabLogin") }}
          </button>
          <button
            data-testid="auth-tab-register"
            class="login-view__tab"
            :class="{ 'login-view__tab--active': activePanel === 'register' }"
            type="button"
            role="tab"
            :aria-selected="activePanel === 'register'"
            @click="activePanel = 'register'"
          >
            {{ t("login.tabRegister") }}
          </button>
        </div>

        <p class="login-view__panel-copy">
          {{ t("login.panelCopy") }}
        </p>
      </div>

      <form
        v-if="activePanel === 'login'"
        class="login-view__form"
        @submit.prevent="handleLogin"
      >
        <FormField
          :label="t('login.email')"
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
          :label="t('login.password')"
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
          {{ isLoading ? t("login.loginLoading") : t("common.actions.login") }}
        </button>
      </form>

      <form
        v-else
        class="login-view__form"
        @submit.prevent="handleRegister"
      >
        <FormField :label="t('login.displayName')">
          <input
            v-model="registerForm.displayName"
            class="login-view__input"
            type="text"
            autocomplete="name"
          >
        </FormField>

        <FormField
          :label="t('login.email')"
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
          :label="t('login.password')"
          :hint="t('login.passwordHint')"
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
          {{ isLoading ? t("login.registerLoading") : t("common.actions.createAccount") }}
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
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  width: 100%;
  max-width: var(--page-max-width);
  margin: 0 auto;
  padding-inline: var(--page-padding);
  gap: 0.25rem;
}

.login-view__toolbar {
  position: absolute;
  top: 0;
  right: var(--page-padding);
  display: flex;
  justify-content: flex-end;
  width: auto;
}

.login-view__intro {
  display: grid;
  gap: 0.75rem;
  width: min(100%, 40rem);
  margin: 0 auto;
  justify-items: center;
  text-align: center;
  justify-self: center;
}

.login-view__brand-lockup {
  display: inline-grid;
  gap: 0.65rem;
  min-width: 0;
  justify-items: center;
}

.login-view__brand-mark {
  width: clamp(8.3rem, 19vw, 10.4rem);
  height: clamp(8.3rem, 19vw, 10.4rem);
}

.login-view__brand-copy {
  display: grid;
  gap: 0.15rem;
}

.login-view__brand-name {
  color: var(--color-text-primary);
  font-size: clamp(1.9rem, 4vw, 2.45rem);
  font-weight: 800;
  letter-spacing: -0.04em;
}

.login-view__brand-subtitle {
  color: var(--color-text-secondary);
  max-width: 20ch;
  font-size: clamp(0.92rem, 1.8vw, 1rem);
  line-height: 1.2;
  white-space: nowrap;
}

.login-view :deep(.page-title) {
  max-width: none;
  font-size: clamp(1.8rem, 6.2vw, 3.2rem);
  line-height: 1.02;
  white-space: nowrap;
  text-wrap: balance;
}

.login-view :deep(.page-copy) {
  max-width: 34rem;
  margin-top: 0;
}

.login-view__panel {
  display: grid;
  gap: 0.95rem;
  width: min(100%, 35.75rem);
  padding: clamp(1rem, 2.2vw, 1.2rem);
  background: var(--theme-login-panel-glow), var(--theme-card-background);
  justify-self: center;
}

.login-view__panel-header {
  display: grid;
  gap: 0.65rem;
}

.login-view__tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.3rem;
  padding: 0.28rem;
  background: var(--theme-login-tabs-background);
  border: 1px solid var(--theme-login-tabs-border);
  border-radius: calc(var(--radius-md) + 2px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 10px 24px rgba(5, 9, 19, 0.08);
}

.login-view__tab {
  position: relative;
  min-height: 2.6rem;
  padding: 0.48rem 0.8rem;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  font-weight: 700;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.login-view__tab--active {
  background: var(--theme-login-tab-active-background);
  color: var(--color-text-primary);
  box-shadow:
    var(--theme-login-tab-active-shadow),
    0 8px 18px rgba(5, 9, 19, 0.07);
  border-color: var(--theme-login-tab-active-border);
}

.login-view__tab:hover {
  background: color-mix(
    in srgb,
    var(--theme-login-tab-active-background) 48%,
    transparent
  );
  color: var(--color-text-primary);
}

.login-view__panel-copy {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.92rem;
}

.login-view__form {
  display: grid;
  gap: 0.95rem;
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
  .login-view__header {
    padding-inline: max(var(--page-padding), 0.9rem);
  }

  .login-view__toolbar {
    right: max(var(--page-padding), 0.9rem);
  }

  .login-view__brand-subtitle {
    max-width: 14ch;
    font-size: 0.9rem;
    white-space: normal;
  }

  .login-view :deep(.page-title) {
    max-width: 11ch;
    white-space: normal;
  }

  .login-view__panel-copy {
    font-size: 0.92rem;
  }
}
</style>
