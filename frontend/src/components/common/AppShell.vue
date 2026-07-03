<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import BrandMark from '@/components/brand/BrandMark.vue';
import ThemePreferenceSwitch from '@/components/common/ThemePreferenceSwitch.vue';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';

const appStore = useAppStore();
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const { appTitle, navigationItems } = storeToRefs(appStore);
const { isAuthenticated, userDisplayName } = storeToRefs(authStore);

const visibleNavigationItems = computed(() =>
  authStore.canAccessProtectedRoutes ? navigationItems.value : [],
);
const isImmersiveRoute = computed(() => route.name === 'swipe');
const showsHeader = computed(() => route.name !== 'login' || isAuthenticated.value);
const showsAuthStatus = computed(
  () => authStore.mode !== 'local-demo' || isAuthenticated.value,
);
const authMenuRef = ref<HTMLElement | null>(null);
const authMenuOpen = ref(false);

const authStatusLabel = computed(() => {
  if (isAuthenticated.value) {
    return userDisplayName.value ?? 'Authenticated';
  }

  return 'Account';
});

async function handleLogout() {
  authMenuOpen.value = false;
  await authStore.logout();
  await router.push({ name: 'login' });
}

onMounted(() => {
  window.addEventListener('pointerdown', handleWindowPointerDown);
  window.addEventListener('keydown', handleWindowKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', handleWindowPointerDown);
  window.removeEventListener('keydown', handleWindowKeydown);
});

function toggleAuthMenu() {
  authMenuOpen.value = !authMenuOpen.value;
}

function handleWindowPointerDown(event: PointerEvent) {
  if (!authMenuOpen.value) {
    return;
  }

  if (!(event.target instanceof Node)) {
    authMenuOpen.value = false;
    return;
  }

  if (!authMenuRef.value?.contains(event.target)) {
    authMenuOpen.value = false;
  }
}

function handleWindowKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && authMenuOpen.value) {
    authMenuOpen.value = false;
  }
}
</script>

<template>
  <div
    class="app-shell"
    :class="{ 'app-shell--immersive': isImmersiveRoute }"
  >
    <header
      v-if="showsHeader"
      class="app-shell__header"
    >
      <div class="app-shell__header-inner page-shell">
        <div class="app-shell__masthead">
          <div class="app-shell__brand-group">
            <RouterLink
              :to="{ name: 'dashboard' }"
              class="app-shell__brand"
              data-testid="brand-link"
            >
              <BrandMark class="app-shell__brand-mark" />
              <span class="app-shell__brand-name">{{ appTitle }}</span>
            </RouterLink>
          </div>

          <div class="app-shell__header-tools">
            <div
              v-if="showsAuthStatus"
              class="app-shell__auth"
            >
              <RouterLink
                v-if="authStore.isAuthRequiredMode && !isAuthenticated"
                :to="{ name: 'login' }"
                class="button button--secondary app-shell__auth-action"
              >
                Anmelden
              </RouterLink>

              <details
                v-else-if="isAuthenticated"
                ref="authMenuRef"
                class="app-shell__auth-menu"
                :open="authMenuOpen"
              >
                <summary
                  class="app-shell__auth-trigger"
                  role="button"
                  :aria-expanded="authMenuOpen ? 'true' : 'false'"
                  @click.prevent="toggleAuthMenu"
                >
                  <span class="app-shell__auth-label">{{ authStatusLabel }}</span>
                  <span
                    class="app-shell__auth-chevron"
                    aria-hidden="true"
                  >▾</span>
                </summary>

                <div class="app-shell__auth-dropdown">
                  <button
                    class="button button--secondary app-shell__auth-dropdown-action"
                    type="button"
                    @click="handleLogout"
                  >
                    Abmelden
                  </button>
                </div>
              </details>
            </div>

            <div
              class="app-shell__preferences"
              data-testid="header-preferences"
            >
              <slot name="header-preferences" />
              <ThemePreferenceSwitch class="app-shell__preference-control" />
            </div>
          </div>
        </div>

        <div
          v-if="visibleNavigationItems.length > 0"
          class="app-shell__nav-frame"
          data-testid="primary-nav-frame"
        >
          <nav
            class="app-shell__nav"
            aria-label="Primary navigation"
          >
            <RouterLink
              v-for="item in visibleNavigationItems"
              :key="item.label"
              :to="item.to"
              class="app-shell__nav-link"
              :title="item.label"
            >
              {{ item.label }}
            </RouterLink>
          </nav>
        </div>
      </div>
    </header>

    <main
      class="page-shell app-shell__content"
      :class="{
        'app-shell__content--immersive': isImmersiveRoute,
        'app-shell__content--headerless': !showsHeader,
      }"
    >
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  background: var(--theme-app-shell-background);
}

.app-shell::before {
  content: '';
  position: fixed;
  inset: 0;
  background: var(--theme-app-shell-overlay);
  pointer-events: none;
}

.app-shell__header {
  position: sticky;
  top: 0;
  z-index: 20;
  padding-top: max(0px, env(safe-area-inset-top));
  background: var(--theme-app-shell-header-background);
  backdrop-filter: blur(18px);
}

.app-shell__header::after {
  content: '';
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 1px;
  background: var(--theme-app-shell-header-rule);
}

.app-shell__header-inner {
  display: grid;
  gap: 0.85rem;
  overflow-x: clip;
  padding-top: 1rem;
  padding-bottom: 1rem;
}

.app-shell__masthead {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  min-width: 0;
}

.app-shell__masthead > * {
  max-width: 100%;
  min-width: 0;
}

.app-shell__brand-group {
  display: flex;
  align-items: center;
  gap: 0.95rem;
  min-width: 0;
}

.app-shell__brand {
  display: inline-flex;
  align-items: center;
  gap: 0.8rem;
  min-width: 0;
  color: var(--color-text-primary);
  font-size: 1.02rem;
  font-weight: 700;
}

.app-shell__brand-mark {
  flex: 0 0 auto;
  width: 2.4rem;
  height: 2.4rem;
  opacity: 0.98;
}

.app-shell__brand-name {
  font-size: 1.08rem;
  letter-spacing: -0.02em;
}

.app-shell__nav-frame {
  overflow: hidden;
  padding: 0.35rem;
  border: 1px solid var(--color-border);
  border-radius: calc(var(--radius-lg) - 2px);
  background: var(--theme-app-shell-frame-background);
  box-shadow: var(--theme-app-shell-frame-shadow);
}

.app-shell__nav {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.45rem;
  min-width: 0;
}

.app-shell__header-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem 0.65rem;
  min-width: 0;
}

.app-shell__preferences {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.35rem;
  min-height: 2.6rem;
  min-width: 0;
}

.app-shell__auth {
  display: flex;
  align-items: center;
  min-width: 0;
}

.app-shell__auth-label {
  display: block;
  max-width: 11rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
  font-size: 0.95rem;
  font-weight: 700;
}

.app-shell__auth-menu {
  position: relative;
}

.app-shell__auth-menu[open] .app-shell__auth-trigger {
  border-color: var(--theme-app-shell-link-active-border);
  background: var(--theme-app-shell-link-active-background);
  box-shadow: var(--theme-app-shell-link-active-shadow);
}

.app-shell__auth-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 2.6rem;
  padding: 0.56rem 0.88rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--theme-app-shell-frame-background);
  color: var(--color-text-primary);
  cursor: pointer;
  list-style: none;
  user-select: none;
}

.app-shell__auth-trigger::-webkit-details-marker {
  display: none;
}

.app-shell__auth-chevron {
  color: var(--color-text-secondary);
  font-size: 0.78rem;
  transition: transform 160ms ease;
}

.app-shell__auth-menu[open] .app-shell__auth-chevron {
  transform: rotate(180deg);
}

.app-shell__auth-dropdown {
  position: absolute;
  top: calc(100% + 0.35rem);
  right: 0;
  z-index: 25;
  min-width: 9.75rem;
  padding: 0.3rem;
  border: 1px solid var(--color-border);
  border-radius: calc(var(--radius-md) + 2px);
  background: var(--theme-app-shell-frame-background);
  box-shadow: var(--theme-app-shell-frame-shadow);
  backdrop-filter: blur(16px);
}

.app-shell__auth-dropdown-action {
  width: 100%;
  min-height: 2.3rem;
  justify-content: flex-start;
  padding-inline: 0.82rem;
  border-radius: calc(var(--radius-sm) + 2px);
  box-shadow: none;
}

.app-shell__nav-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.75rem;
  padding: 0.66rem 0.95rem;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  white-space: nowrap;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    transform 160ms ease;
}

.app-shell__nav-link:hover {
  background: var(--theme-app-shell-link-hover-background);
  border-color: var(--theme-app-shell-link-hover-border);
  color: var(--color-text-primary);
  transform: translateY(-1px);
}

.app-shell__nav-link.router-link-active {
  background: var(--theme-app-shell-link-active-background);
  border-color: var(--theme-app-shell-link-active-border);
  color: var(--theme-app-shell-link-active-color);
  box-shadow: var(--theme-app-shell-link-active-shadow);
}

.app-shell__content {
  position: relative;
  z-index: 1;
  padding-top: clamp(0.9rem, 2vw, 1.35rem);
  padding-bottom: calc(2.4rem + env(safe-area-inset-bottom));
}

.app-shell__content--immersive {
  padding-top: 0.75rem;
}

.app-shell__content--headerless {
  padding-top: max(1.1rem, env(safe-area-inset-top));
}

@media (max-width: 780px) {
  .app-shell__masthead {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: start;
  }

  .app-shell__brand-group {
    width: 100%;
  }

  .app-shell__header-tools {
    width: auto;
    flex-wrap: nowrap;
    justify-content: flex-end;
    gap: 0.65rem;
    justify-self: end;
  }

  .app-shell__preferences {
    width: auto;
    justify-content: flex-end;
    flex-wrap: nowrap;
  }

  .app-shell__nav {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 0.1rem;
    scrollbar-width: none;
  }

  .app-shell__nav::-webkit-scrollbar {
    display: none;
  }

  .app-shell__nav-frame {
    padding: 0.28rem;
  }

  .app-shell__nav-link {
    min-height: 2.55rem;
    padding: 0.58rem 0.78rem;
    font-size: 0.92rem;
  }
}

@media (max-width: 560px) {
  .app-shell__header-inner {
    gap: 0.55rem;
    padding-top: 0.72rem;
    padding-bottom: 0.68rem;
  }

  .app-shell__masthead {
    gap: 0.65rem;
  }

  .app-shell__brand-group {
    align-items: center;
    justify-content: flex-start;
  }

  .app-shell__brand-mark {
    width: 2.2rem;
    height: 2.2rem;
  }

  .app-shell__brand-name {
    font-size: 1rem;
  }

  .app-shell__header-tools {
    flex-wrap: wrap;
    gap: 0.4rem;
  }

  .app-shell__preferences {
    justify-content: flex-end;
  }

  .app-shell__auth-trigger {
    min-height: 2.4rem;
    padding: 0.52rem 0.78rem;
  }

  .app-shell__auth-label {
    max-width: 7.5rem;
  }

  .app-shell__nav-link {
    min-height: 2.4rem;
    padding: 0.52rem 0.72rem;
    font-size: 0.88rem;
  }
}
</style>
