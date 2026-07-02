<script setup lang="ts">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { RouterLink, useRouter } from 'vue-router';

import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';

const appStore = useAppStore();
const authStore = useAuthStore();
const router = useRouter();
const { appTitle, navigationItems } = storeToRefs(appStore);
const { isAuthenticated, userDisplayName } = storeToRefs(authStore);

const visibleNavigationItems = computed(() =>
  authStore.canAccessProtectedRoutes ? navigationItems.value : [],
);

const authStatusLabel = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'Local demo mode';
  }

  if (isAuthenticated.value) {
    return userDisplayName.value ?? 'Authenticated';
  }

  return 'Signed out';
});

const authStatusDescription = computed(() => {
  if (authStore.mode === 'local-demo') {
    return 'Private routes stay open for local development.';
  }

  if (isAuthenticated.value) {
    return 'Your private media and matches are tied to this account.';
  }

  return 'Protected routes redirect to login until you sign in.';
});

async function handleLogout() {
  authStore.logout();
  await router.push({ name: 'login' });
}
</script>

<template>
  <div class="app-shell">
    <header class="app-shell__header">
      <div class="app-shell__header-inner page-shell">
        <RouterLink
          :to="{ name: 'dashboard' }"
          class="app-shell__brand"
        >
          <span class="app-shell__brand-mark">MM</span>
          <span>{{ appTitle }}</span>
        </RouterLink>

        <nav
          v-if="visibleNavigationItems.length > 0"
          class="app-shell__nav"
          aria-label="Primary navigation"
        >
          <RouterLink
            v-for="item in visibleNavigationItems"
            :key="item.label"
            :to="item.to"
            class="app-shell__nav-link"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="app-shell__auth">
          <div class="app-shell__auth-copy">
            <span class="app-shell__auth-label">{{ authStatusLabel }}</span>
            <span class="app-shell__auth-description">{{ authStatusDescription }}</span>
          </div>

          <RouterLink
            v-if="authStore.isAuthRequiredMode && !isAuthenticated"
            :to="{ name: 'login' }"
            class="button button--secondary app-shell__auth-action"
          >
            Anmelden
          </RouterLink>

          <button
            v-else-if="isAuthenticated"
            class="button button--secondary app-shell__auth-action"
            type="button"
            @click="handleLogout"
          >
            Abmelden
          </button>
        </div>
      </div>
    </header>

    <main class="page-shell app-shell__content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--color-background);
}

.app-shell__header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--color-surface-secondary);
  border-bottom: 1px solid var(--color-border);
}

.app-shell__header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  min-height: 4.5rem;
}

.app-shell__brand {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--color-text-primary);
  font-size: 1rem;
  font-weight: 700;
}

.app-shell__brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-full);
  background: var(--color-accent-soft);
  color: var(--color-accent-dark);
  border: 1px solid var(--color-border);
}

.app-shell__nav {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.app-shell__auth {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.app-shell__auth-copy {
  display: grid;
  gap: 0.15rem;
  text-align: right;
}

.app-shell__auth-label {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.app-shell__auth-description {
  max-width: 20rem;
  color: var(--color-text-secondary);
  font-size: 0.84rem;
  line-height: 1.4;
}

.app-shell__nav-link {
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: background-color 160ms ease, color 160ms ease;
}

.app-shell__nav-link:hover,
.app-shell__nav-link.router-link-active {
  background: var(--color-accent-soft);
  color: var(--color-accent-dark);
}

.app-shell__content {
  padding-top: 2rem;
  padding-bottom: 2rem;
}

@media (max-width: 640px) {
  .app-shell__header-inner {
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    padding-top: 1rem;
    padding-bottom: 1rem;
  }

  .app-shell__auth {
    width: 100%;
    justify-content: space-between;
  }

  .app-shell__auth-copy {
    text-align: left;
  }
}
</style>
