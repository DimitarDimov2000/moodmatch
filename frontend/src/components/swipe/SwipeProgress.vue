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
          Lokale Runde
        </p>
        <h2 class="section-title">
          {{ remainingCount > 0 ? `Karte ${currentPosition} von ${totalCount}` : `Alle ${totalCount} Karten bearbeitet` }}
        </h2>
      </div>

      <div class="swipe-progress__pill">
        {{ progressPercent }} % durchlaufen
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
        <dt>Abgelehnt</dt>
        <dd>{{ stats.rejected }}</dd>
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
  padding: 1.25rem;
}

.swipe-progress__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.swipe-progress__pill {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--color-accent-soft) 70%, var(--color-surface));
  color: var(--color-accent-dark);
  font-size: 0.9rem;
  font-weight: 600;
}

.swipe-progress__bar {
  height: 0.7rem;
  overflow: hidden;
  border-radius: var(--radius-full);
  background: var(--color-surface-muted);
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
}

.swipe-progress__stats div {
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-secondary);
}

.swipe-progress__stats dt {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.swipe-progress__stats dd {
  margin: 0.35rem 0 0;
  font-size: 1.2rem;
  font-weight: 700;
}

@media (max-width: 820px) {
  .swipe-progress__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .swipe-progress__stats {
    grid-template-columns: 1fr;
  }
}
</style>
