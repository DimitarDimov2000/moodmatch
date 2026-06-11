<script setup lang="ts">
import { computed } from 'vue';

import { formatPercentage, getScoreTone } from '@/components/matching/matching-format';

const props = withDefaults(
  defineProps<{
    score: string | null;
    suppressed?: boolean;
    insufficientLabel?: string;
  }>(),
  {
    suppressed: false,
    insufficientLabel: 'Kein aussagekraeftiger Score',
  },
);

const scoreLabel = computed(() => formatPercentage(props.score));
const toneClass = computed(() => `match-score-display--${getScoreTone(props.score)}`);
const detailLabel = computed(() =>
  props.score === null || props.suppressed ? props.insufficientLabel : 'Relativer Match-Score',
);
</script>

<template>
  <div
    class="match-score-display"
    :class="toneClass"
  >
    <p class="match-score-display__eyebrow">
      Match-Score
    </p>
    <p class="match-score-display__value">
      {{ scoreLabel ?? 'Keine Prozentangabe' }}
    </p>
    <p class="match-score-display__detail">
      {{ detailLabel }}
    </p>
  </div>
</template>

<style scoped>
.match-score-display {
  display: grid;
  gap: 0.2rem;
  min-width: 10rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface-secondary);
}

.match-score-display--success {
  background: var(--color-success-soft);
  border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border));
}

.match-score-display--accent {
  background: var(--color-accent-soft);
  border-color: color-mix(in srgb, var(--color-accent) 30%, var(--color-border));
}

.match-score-display--warning {
  background: var(--color-warning-soft);
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
}

.match-score-display--muted {
  color: var(--color-text-secondary);
}

.match-score-display__eyebrow,
.match-score-display__value,
.match-score-display__detail {
  margin: 0;
}

.match-score-display__eyebrow {
  color: var(--color-text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.match-score-display__value {
  font-size: 1.65rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.match-score-display__detail {
  font-size: 0.92rem;
  color: var(--color-text-secondary);
}
</style>
