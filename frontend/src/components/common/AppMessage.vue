<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  tone?: 'neutral' | 'error' | 'warning' | 'info';
}>();
</script>

<template>
  <div
    class="app-message page-card"
    :class="`app-message--${tone ?? 'neutral'}`"
  >
    <h2 class="app-message__title">
      {{ title }}
    </h2>
    <p class="app-message__description">
      {{ description }}
    </p>
    <slot />
  </div>
</template>

<style scoped>
.app-message {
  padding: 1.25rem;
}

.app-message--neutral {
  background: var(--color-surface-secondary);
}

.app-message--error {
  background: var(--color-error-soft);
  border-color: color-mix(in srgb, var(--color-error) 30%, var(--color-border));
}

.app-message--warning {
  background: var(--color-warning-soft);
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
}

.app-message--info {
  background: var(--color-info-soft);
  border-color: color-mix(in srgb, var(--color-info) 30%, var(--color-border));
}

.app-message__title {
  margin: 0;
  font-size: 1rem;
}

.app-message__description {
  margin: 0.5rem 0 0;
  color: var(--color-text-secondary);
}
</style>

