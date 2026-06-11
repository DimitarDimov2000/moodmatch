<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { RouterLink } from 'vue-router';

import { useAppStore } from '@/stores/app';

const appStore = useAppStore();
const { appTitle, navigationItems } = storeToRefs(appStore);
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
          class="app-shell__nav"
          aria-label="Primary navigation"
        >
          <RouterLink
            v-for="item in navigationItems"
            :key="item.label"
            :to="item.to"
            class="app-shell__nav-link"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
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
}
</style>
