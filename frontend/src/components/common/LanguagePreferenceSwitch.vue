<script setup lang="ts">
import { computed } from 'vue';

import { i18n } from '@/i18n';
import { useLocale } from '@/i18n';

const { t } = i18n.global;
const { locale, nextLocale, setLocale } = useLocale();

const currentLabel = computed(() => t(`preferences.language.${locale.value}`));
const currentName = computed(() => t(`preferences.language.name.${locale.value}`));
const nextName = computed(() => t(`preferences.language.name.${nextLocale.value}`));
const actionLabel = computed(() =>
  t('preferences.language.action', {
    current: currentName.value,
    next: nextName.value,
  }),
);

function cycleLanguage() {
  setLocale(nextLocale.value);
}
</script>

<template>
  <button
    class="language-switch"
    data-testid="language-switch"
    type="button"
    :aria-label="actionLabel"
    :title="actionLabel"
    @click="cycleLanguage"
  >
    <span class="language-switch__text">{{ currentLabel }}</span>
  </button>
</template>

<style scoped>
.language-switch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 2.5rem;
  padding: 0.42rem 0.72rem;
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

.language-switch:hover {
  background: var(--theme-control-button-active-background);
  border-color: var(--theme-control-button-active-border);
  color: var(--theme-control-button-active-color);
  box-shadow: var(--theme-control-button-active-shadow);
  transform: translateY(-1px);
}

.language-switch:active {
  transform: translateY(0);
}

.language-switch__text {
  font-size: 0.84rem;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .language-switch {
    min-height: 2.35rem;
    padding-inline: 0.58rem;
  }
}
</style>
