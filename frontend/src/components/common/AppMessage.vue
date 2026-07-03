<script setup lang="ts">
const props = withDefaults(defineProps<{
  title: string;
  description: string;
  tone?: 'neutral' | 'error' | 'warning' | 'info';
}>(), {
  tone: 'neutral',
});
</script>

<template>
  <div
    class="app-message page-card state-panel"
    :class="`app-message--${props.tone}`"
    :role="props.tone === 'error' ? 'alert' : 'status'"
  >
    <h2 class="app-message__title">
      {{ props.title }}
    </h2>
    <p class="app-message__description">
      {{ props.description }}
    </p>
    <slot />
  </div>
</template>

<style scoped>
.app-message {
  padding: clamp(1rem, 2.5vw, 1.25rem);
}

.app-message::before {
  content: '';
  position: absolute;
  top: 0.95rem;
  bottom: 0.95rem;
  left: 0.9rem;
  width: 3px;
  border-radius: var(--radius-full);
  background: var(--theme-message-rail-background);
}

.app-message--neutral {
  background: var(--theme-message-neutral-background);
}

.app-message--neutral::before {
  background: color-mix(in srgb, var(--color-info) 60%, white 8%);
}

.app-message--error {
  background: var(--theme-message-error-background);
  border-color: color-mix(in srgb, var(--color-error) 30%, var(--color-border));
}

.app-message--error::before {
  background: var(--color-error);
}

.app-message--warning {
  background: var(--theme-message-warning-background);
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
}

.app-message--warning::before {
  background: var(--color-warning);
}

.app-message--info {
  background: var(--theme-message-info-background);
  border-color: color-mix(in srgb, var(--color-info) 30%, var(--color-border));
}

.app-message--info::before {
  background: var(--color-info);
}

.app-message__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.app-message__description {
  max-width: 72ch;
  margin: 0;
  color: var(--color-text-secondary);
}
</style>
