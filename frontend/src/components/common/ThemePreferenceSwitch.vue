<script setup lang="ts">
import { computed } from 'vue';

import {
  initializeTheme,
  useTheme,
  type ThemePreference,
} from '@/composables/useTheme';
import { i18n } from '@/i18n';

const themeOrder: ThemePreference[] = ['dark', 'light', 'system'];
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const { preference, resolvedTheme, setThemePreference } = useTheme();

initializeTheme();

const nextPreference = computed<ThemePreference>(() => {
  const currentIndex = themeOrder.indexOf(preference.value);
  return themeOrder[(currentIndex + 1) % themeOrder.length];
});

const currentLabel = computed(() => {
  trackLocaleDependency();
  return t(`preferences.theme.${preference.value}`);
});
const nextLabel = computed(() => {
  trackLocaleDependency();
  return t(`preferences.theme.${nextPreference.value}`);
});
const resolvedSystemLabel = computed(() => {
  trackLocaleDependency();
  return resolvedTheme.value === 'dark' ? t('preferences.theme.dark') : t('preferences.theme.light');
});
const actionLabel = computed(() =>
  preference.value === 'system'
    ? t('preferences.theme.actionSystem', {
      current: currentLabel.value,
      resolved: resolvedSystemLabel.value,
      next: nextLabel.value,
    })
    : t('preferences.theme.action', {
      current: currentLabel.value,
      next: nextLabel.value,
    }),
);

function cycleThemePreference() {
  setThemePreference(nextPreference.value);
}
</script>

<template>
  <button
    class="theme-switch"
    data-testid="theme-switch"
    type="button"
    :aria-label="actionLabel"
    :title="actionLabel"
    @click="cycleThemePreference"
  >
    <span
      class="theme-switch__icon"
      aria-hidden="true"
    >
      <svg
        v-if="preference === 'dark'"
        viewBox="0 0 20 20"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M14.64 13.32A6.38 6.38 0 0 1 6.68 5.36 6.62 6.62 0 1 0 14.64 13.32Z"
          fill="currentColor"
        />
      </svg>
      <svg
        v-else-if="preference === 'light'"
        viewBox="0 0 20 20"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M10 3.35a.85.85 0 0 1 .85.85v.86a.85.85 0 0 1-1.7 0V4.2A.85.85 0 0 1 10 3.35Zm0 10.74a.85.85 0 0 1 .85.85v.86a.85.85 0 1 1-1.7 0v-.86a.85.85 0 0 1 .85-.85Zm5.8-4.94a.85.85 0 0 1 0 1.7h-.86a.85.85 0 1 1 0-1.7h.86Zm-10.74 0a.85.85 0 0 1 0 1.7H4.2a.85.85 0 0 1 0-1.7h.86ZM14.1 5.9a.85.85 0 0 1 1.2 0l.6.6a.85.85 0 0 1-1.2 1.2l-.6-.6a.85.85 0 0 1 0-1.2Zm-8.8 8.8a.85.85 0 0 1 1.2 0l.6.6a.85.85 0 1 1-1.2 1.2l-.6-.6a.85.85 0 0 1 0-1.2Zm10 .6a.85.85 0 0 1 0-1.2l.6-.6a.85.85 0 1 1 1.2 1.2l-.6.6a.85.85 0 0 1-1.2 0Zm-8.8-8.8a.85.85 0 0 1 0-1.2l.6-.6a.85.85 0 1 1 1.2 1.2l-.6.6a.85.85 0 0 1-1.2 0ZM10 6.15a3.85 3.85 0 1 1 0 7.7 3.85 3.85 0 0 1 0-7.7Z"
          fill="currentColor"
        />
      </svg>
      <svg
        v-else
        viewBox="0 0 20 20"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M5.75 4.25A2.75 2.75 0 0 0 3 7v5.25A2.75 2.75 0 0 0 5.75 15h8.5A2.75 2.75 0 0 0 17 12.25V7a2.75 2.75 0 0 0-2.75-2.75h-8.5Zm-.5 2h9a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-9a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Zm3 9.5a.75.75 0 0 1 .75-.75h2a.75.75 0 0 1 0 1.5H9a.75.75 0 0 1-.75-.75Z"
          fill="currentColor"
        />
      </svg>
    </span>

    <span class="theme-switch__text">{{ currentLabel }}</span>
  </button>
</template>

<style scoped>
.theme-switch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  min-width: 0;
  min-height: 2.5rem;
  padding: 0.42rem 0.82rem;
  border: 1px solid var(--theme-control-border);
  border-radius: var(--radius-full);
  background: var(--theme-control-background);
  color: var(--theme-control-button-color);
  box-shadow: var(--theme-control-shadow);
  backdrop-filter: blur(16px);
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.theme-switch:hover {
  background: var(--theme-control-button-active-background);
  border-color: var(--theme-control-button-active-border);
  color: var(--theme-control-button-active-color);
  box-shadow: var(--theme-control-button-active-shadow);
  transform: translateY(-1px);
}

.theme-switch:active {
  transform: translateY(0);
}

.theme-switch__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: var(--radius-full);
  background: var(--theme-control-button-hover-background);
  color: currentColor;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.theme-switch__icon svg {
  width: 1rem;
  height: 1rem;
}

.theme-switch__text {
  font-size: 0.84rem;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .theme-switch {
    min-height: 2.35rem;
    padding-inline: 0.58rem;
  }

  .theme-switch__text {
    display: none;
  }
}
</style>
