<script setup lang="ts">
import { computed } from 'vue';

import type { SwipeQueueStats } from '@/types/swipe';

const props = defineProps<{
  totalCount: number;
  remainingCount: number;
  stats: SwipeQueueStats;
}>();

const handledCount = computed(() => props.totalCount - props.remainingCount);
const currentPosition = computed(() =>
  props.remainingCount > 0 ? handledCount.value + 1 : props.totalCount,
);
const progressPercent = computed(() => {
  if (props.totalCount === 0) {
    return 0;
  }

  return Math.round((handledCount.value / props.totalCount) * 100);
});
</script>

<template>
  <section class="swipe-progress page-card">
    <div class="swipe-progress__header">
      <div>
        <p class="eyebrow">
          Swipe-Runde
        </p>
        <h2 class="section-title">
          {{ remainingCount > 0 ? `Karte ${currentPosition} von ${totalCount}` : `Alle ${totalCount} Titel einsortiert` }}
        </h2>
      </div>

      <div class="swipe-progress__pill">
        {{ progressPercent }} % geschafft
      </div>
    </div>

    <div
      class="swipe-progress__bar"
      aria-hidden="true"
    >
      <span
        class="swipe-progress__bar-fill"
        :style="{ width: `${progressPercent}%` }"
      />
    </div>

    <dl class="swipe-progress__stats">
      <div>
        <dt>Verbleibend</dt>
        <dd>{{ remainingCount }}</dd>
      </div>
      <div>
        <dt>Geliket</dt>
        <dd>{{ stats.liked }}</dd>
      </div>
      <div>
        <dt>Uebersprungen</dt>
        <dd>{{ stats.skipped }}</dd>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.swipe-progress {
  display: grid;
  gap: 1rem;
  padding: 1.25rem 1.25rem 1.1rem;
  border-color: var(--theme-swipe-panel-border);
  background: var(--theme-swipe-panel-background);
  backdrop-filter: blur(18px);
}

.swipe-progress__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.swipe-progress .eyebrow {
  color: var(--theme-swipe-eyebrow-color);
}

.swipe-progress .section-title {
  color: var(--theme-swipe-panel-emphasis-text);
}

.swipe-progress__pill {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--theme-swipe-panel-strong-border);
  border-radius: var(--radius-full);
  background: var(--theme-swipe-panel-pill-background);
  color: var(--theme-swipe-panel-pill-color);
  font-size: 0.9rem;
  font-weight: 600;
}

.swipe-progress__bar {
  height: 0.7rem;
  overflow: hidden;
  border-radius: var(--radius-full);
  background: var(--theme-swipe-panel-pill-background);
}

.swipe-progress__bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(
    90deg,
    var(--color-accent),
    color-mix(in srgb, var(--color-success) 45%, var(--color-accent))
  );
  transition: width 180ms ease;
}

.swipe-progress__stats {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
}

.swipe-progress__stats div {
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--theme-swipe-panel-border);
  border-radius: var(--radius-md);
  background: var(--theme-swipe-panel-soft-background);
}

.swipe-progress__stats dt {
  color: var(--theme-swipe-panel-soft-text);
  font-size: 0.82rem;
}

.swipe-progress__stats dd {
  margin: 0.35rem 0 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--theme-swipe-panel-emphasis-text);
}

@media (max-width: 820px) {
  .swipe-progress__stats {
    grid-template-columns: 1fr;
  }
}
</style>
