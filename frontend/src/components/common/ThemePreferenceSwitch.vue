<script setup lang="ts">
import { computed } from 'vue';

import {
  initializeTheme,
  useTheme,
  type ThemePreference,
} from '@/composables/useTheme';

const options: ReadonlyArray<{ value: ThemePreference; label: string }> = [
  { value: 'system', label: 'System' },
  { value: 'dark', label: 'Dark' },
  { value: 'light', label: 'Light' },
];

const { preference, resolvedTheme, setThemePreference } = useTheme();

initializeTheme();

const groupLabel = computed(() =>
  preference.value === 'system'
    ? `Theme preference, following system (${resolvedTheme.value})`
    : `Theme preference, ${preference.value} selected`,
);
</script>

<template>
  <section
    class="theme-switch"
    data-testid="theme-switch"
  >
    <span class="theme-switch__label">Theme</span>

    <div
      class="theme-switch__options"
      role="radiogroup"
      :aria-label="groupLabel"
    >
      <button
        v-for="option in options"
        :key="option.value"
        class="theme-switch__option"
        :class="{ 'theme-switch__option--active': preference === option.value }"
        type="button"
        role="radio"
        :aria-checked="preference === option.value"
        :data-testid="`theme-option-${option.value}`"
        @click="setThemePreference(option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.theme-switch {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
}

.theme-switch__label {
  color: var(--theme-control-label-color);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.theme-switch__options {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  min-width: 0;
  padding: 0.22rem;
  border: 1px solid var(--theme-control-border);
  border-radius: var(--radius-full);
  background: var(--theme-control-background);
  box-shadow: var(--theme-control-shadow);
  backdrop-filter: blur(16px);
}

.theme-switch__option {
  min-height: 2.15rem;
  padding: 0.45rem 0.72rem;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  color: var(--theme-control-button-color);
  font-size: 0.84rem;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.theme-switch__option:hover {
  background: var(--theme-control-button-hover-background);
  color: var(--color-text-primary);
}

.theme-switch__option--active {
  background: var(--theme-control-button-active-background);
  border-color: var(--theme-control-button-active-border);
  color: var(--theme-control-button-active-color);
  box-shadow: var(--theme-control-button-active-shadow);
}

.theme-switch__option--active:hover {
  background: var(--theme-control-button-active-background);
  color: var(--theme-control-button-active-color);
}

@media (max-width: 560px) {
  .theme-switch__label {
    display: none;
  }

  .theme-switch__option {
    min-height: 2.05rem;
    padding-inline: 0.64rem;
    font-size: 0.8rem;
  }
}
</style>
