<script setup lang="ts">
const props = defineProps<{
  pending?: boolean;
  detailsExpanded?: boolean;
}>();

const emit = defineEmits<{
  like: [];
  skip: [];
  details: [];
}>();
</script>

<template>
  <section class="swipe-decision-controls page-card">
    <div class="swipe-decision-controls__copy">
      <p class="eyebrow">
        Schnell entscheiden
      </p>
      <p class="body-muted">
        Links fuer Nicht jetzt, Mitte fuer Details, rechts fuer Like.
      </p>
    </div>

    <div
      class="swipe-decision-controls__buttons"
      role="group"
      aria-label="Swipe-Aktionen"
    >
      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--skip"
        type="button"
        aria-label="Empfehlung vorerst ablehnen"
        :disabled="props.pending"
        @click="emit('skip')"
      >
        <span
          class="swipe-decision-controls__icon"
          aria-hidden="true"
        >×</span>
        <span class="swipe-decision-controls__label">Nicht jetzt</span>
        <span class="swipe-decision-controls__hint">Links</span>
      </button>

      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--details"
        type="button"
        :aria-expanded="props.detailsExpanded ? 'true' : 'false'"
        aria-label="Empfehlungsdetails ein- oder ausklappen"
        :disabled="props.pending"
        @click="emit('details')"
      >
        <span
          class="swipe-decision-controls__icon"
          aria-hidden="true"
        >⌄</span>
        <span class="swipe-decision-controls__label">
          {{ props.detailsExpanded ? 'Weniger' : 'Details' }}
        </span>
        <span class="swipe-decision-controls__hint">Enter</span>
      </button>

      <button
        class="button swipe-decision-controls__button swipe-decision-controls__button--like"
        type="button"
        aria-label="Empfehlung liken"
        :disabled="props.pending"
        @click="emit('like')"
      >
        <span
          class="swipe-decision-controls__icon"
          aria-hidden="true"
        >→</span>
        <span class="swipe-decision-controls__label">Like</span>
        <span class="swipe-decision-controls__hint">Rechts</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.swipe-decision-controls {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(10, 16, 36, 0.76);
  backdrop-filter: blur(18px);
}

.swipe-decision-controls__copy {
  display: grid;
  gap: 0.3rem;
}

.swipe-decision-controls__copy p {
  margin: 0;
}

.swipe-decision-controls__copy .eyebrow {
  color: #ffb4b8;
}

.swipe-decision-controls__copy .body-muted {
  color: rgba(236, 239, 255, 0.72);
}

.swipe-decision-controls__buttons {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.swipe-decision-controls__button {
  display: grid;
  justify-items: center;
  gap: 0.15rem;
  min-height: 5rem;
  padding: 0.9rem 0.65rem;
  border-radius: 999px;
  border-width: 1px;
}

.swipe-decision-controls__icon {
  font-size: 1.15rem;
  font-weight: 700;
}

.swipe-decision-controls__label {
  font-weight: 700;
}

.swipe-decision-controls__hint {
  color: rgba(236, 239, 255, 0.6);
  font-size: 0.82rem;
}

.swipe-decision-controls__button--skip {
  background: rgba(255, 182, 193, 0.1);
  border-color: rgba(255, 182, 193, 0.22);
  color: #ffb4b8;
}

.swipe-decision-controls__button--details {
  background: rgba(190, 196, 255, 0.1);
  border-color: rgba(190, 196, 255, 0.22);
  color: #c7c5ff;
}

.swipe-decision-controls__button--like {
  background: linear-gradient(180deg, rgba(255, 74, 124, 0.92), rgba(229, 49, 98, 0.92));
  border-color: rgba(255, 255, 255, 0.18);
  color: #fff;
  box-shadow: 0 12px 24px rgba(229, 49, 98, 0.24);
}

@media (max-width: 640px) {
  .swipe-decision-controls {
    position: sticky;
    bottom: max(0.65rem, env(safe-area-inset-bottom));
    z-index: 2;
    box-shadow: 0 16px 34px rgba(4, 10, 24, 0.34);
  }

  .swipe-decision-controls__copy {
    display: none;
  }
}

@media (max-width: 380px) {
  .swipe-decision-controls__button {
    min-height: 4.35rem;
    padding-inline: 0.45rem;
  }

  .swipe-decision-controls__label {
    font-size: 0.92rem;
  }

  .swipe-decision-controls__hint {
    font-size: 0.76rem;
  }
}
</style>
