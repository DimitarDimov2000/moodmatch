<script setup lang="ts">
const props = defineProps<{
  pending?: boolean;
  rejectPersists?: boolean;
}>();

const emit = defineEmits<{
  like: [];
  reject: [];
  skip: [];
  details: [];
}>();
</script>

<template>
  <section class="swipe-decision-controls page-card">
    <div class="swipe-decision-controls__copy">
      <p class="eyebrow">
        Aktionen
      </p>
      <h2 class="section-title">
        Schnell entscheiden
      </h2>
      <p class="body-muted">
        Liken und Ueberspringen bleiben in dieser Runde lokal. Ablehnen
        {{ rejectPersists ? 'setzt den Status auf Kein Interesse.' : 'bleibt vorerst lokal.' }}
      </p>
    </div>

    <div
      class="swipe-decision-controls__buttons"
      role="group"
      aria-label="Entscheidungsaktionen"
    >
      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--reject"
        type="button"
        :disabled="props.pending"
        @click="emit('reject')"
      >
        <span aria-hidden="true">×</span>
        <span>Ablehnen</span>
        <kbd>←</kbd>
      </button>

      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--skip"
        type="button"
        :disabled="props.pending"
        @click="emit('skip')"
      >
        <span aria-hidden="true">…</span>
        <span>Ueberspringen</span>
        <kbd>↓</kbd>
        <kbd>S</kbd>
      </button>

      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--details"
        type="button"
        :disabled="props.pending"
        @click="emit('details')"
      >
        <span>Details</span>
        <kbd>Enter</kbd>
      </button>

      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--like"
        type="button"
        :disabled="props.pending"
        @click="emit('like')"
      >
        <span aria-hidden="true">♡</span>
        <span>Liken</span>
        <kbd>→</kbd>
      </button>
    </div>
  </section>
</template>

<style scoped>
.swipe-decision-controls {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
}

.swipe-decision-controls__copy {
  display: grid;
  gap: 0.45rem;
}

.swipe-decision-controls__copy p {
  margin: 0;
}

.swipe-decision-controls__buttons {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.swipe-decision-controls__button {
  justify-content: space-between;
  min-height: 3.15rem;
  padding-inline: 0.9rem;
}

.swipe-decision-controls__button--reject {
  background: var(--color-error-soft);
  border-color: color-mix(in srgb, var(--color-error) 30%, var(--color-border));
  color: var(--color-error);
}

.swipe-decision-controls__button--skip {
  background: var(--color-surface-secondary);
  border-color: var(--color-border);
  color: var(--color-text-secondary);
}

.swipe-decision-controls__button--details {
  background: var(--color-info-soft);
  border-color: color-mix(in srgb, var(--color-info) 25%, var(--color-border));
  color: var(--color-info);
}

.swipe-decision-controls__button--like {
  background: var(--color-success-soft);
  border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  color: var(--color-success);
}

kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2rem;
  min-height: 1.75rem;
  padding: 0.15rem 0.45rem;
  border: 1px solid color-mix(in srgb, currentColor 18%, var(--color-border));
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-surface) 88%, transparent);
  color: inherit;
  font-size: 0.8rem;
  font-family: var(--font-sans);
}

@media (max-width: 980px) {
  .swipe-decision-controls__buttons {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .swipe-decision-controls__buttons {
    grid-template-columns: 1fr;
  }
}
</style>
