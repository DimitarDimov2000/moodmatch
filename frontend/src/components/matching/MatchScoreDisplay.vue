<script setup lang="ts">
import { computed } from "vue";

import {
  formatPercentage,
  getScoreTone,
} from "@/components/matching/matching-format";
import { i18n } from "@/i18n";

const props = withDefaults(
  defineProps<{
    score: number | string | null;
    suppressed?: boolean;
    insufficientLabel?: string;
    compact?: boolean;
  }>(),
  {
    suppressed: false,
    insufficientLabel: undefined,
    compact: false,
  },
);
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const scoreLabel = computed(() => {
  trackLocaleDependency();
  return props.suppressed ? null : formatPercentage(props.score);
});
const toneClass = computed(
  () => `match-score-display--${props.suppressed ? 'muted' : getScoreTone(props.score)}`,
);
const detailLabel = computed(() => {
  trackLocaleDependency();
  return props.score === null || props.suppressed
    ? props.insufficientLabel ?? t("matching.insufficient")
    : t("matching.relativeScore");
});
</script>

<template>
  <div
    class="match-score-display"
    :class="[toneClass, { 'match-score-display--compact': compact }]"
  >
    <div class="match-score-display__inner">
      <p class="match-score-display__eyebrow">
        {{ t("matching.scoreEyebrow") }}
      </p>
      <p class="match-score-display__value">
        {{ scoreLabel ?? t("common.states.noPercentage") }}
      </p>
      <p class="match-score-display__detail">
        {{ detailLabel }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.match-score-display {
  position: relative;
  isolation: isolate;
  display: grid;
  min-width: min(100%, 9.5rem);
  max-width: min(100%, 12.25rem);
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 70%, transparent);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface-secondary) 90%, transparent);
  box-shadow: 0 8px 20px rgba(5, 9, 19, 0.1);
}

.match-score-display::before {
  content: "";
  position: absolute;
  inset: 0.62rem 0.72rem auto auto;
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  filter: blur(14px);
  opacity: 0.55;
  pointer-events: none;
}

.match-score-display__inner {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.2rem;
  min-height: 5.7rem;
  padding: 0.72rem 0.82rem 0.8rem;
  border-radius: inherit;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.14), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 28%),
    color-mix(in srgb, var(--color-surface-strong) 84%, var(--color-surface));
  align-content: start;
}

.match-score-display--success {
  border-color: color-mix(in srgb, var(--color-success) 28%, var(--color-border));
  background: color-mix(in srgb, var(--color-success-soft) 42%, var(--color-surface));
}

.match-score-display--success .match-score-display__inner {
  background:
    radial-gradient(circle at top right, rgba(159, 240, 208, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 28%),
    color-mix(in srgb, var(--color-success-soft) 72%, var(--color-surface-strong));
}

.match-score-display--low {
  border-color: color-mix(in srgb, var(--color-error) 32%, var(--color-border));
  background: color-mix(in srgb, var(--color-error-soft) 40%, var(--color-surface));
}

.match-score-display--low .match-score-display__inner {
  background:
    radial-gradient(circle at top right, rgba(255, 184, 184, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 28%),
    color-mix(in srgb, var(--color-error-soft) 68%, var(--color-surface-strong));
}

.match-score-display--warning {
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
  background: color-mix(in srgb, var(--color-warning-soft) 42%, var(--color-surface));
}

.match-score-display--warning .match-score-display__inner {
  background:
    radial-gradient(circle at top right, rgba(255, 210, 154, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 28%),
    color-mix(in srgb, var(--color-warning-soft) 70%, var(--color-surface-strong));
}

.match-score-display--muted {
  color: color-mix(in srgb, #607188 84%, var(--color-text-secondary));
  border-color: color-mix(in srgb, #7f90a8 28%, var(--color-border));
  background: color-mix(in srgb, #dbe4ef 22%, var(--color-surface));
  box-shadow: 0 6px 16px rgba(12, 18, 30, 0.05);
}

.match-score-display--muted .match-score-display__inner {
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.1), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 28%),
    color-mix(in srgb, #eef3f8 78%, var(--color-surface));
}

.match-score-display__eyebrow,
.match-score-display__value,
.match-score-display__detail {
  margin: 0;
}

.match-score-display__eyebrow {
  color: var(--color-text-secondary);
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.match-score-display__value {
  font-size: clamp(1.5rem, 3vw, 1.95rem);
  font-weight: 800;
  letter-spacing: -0.06em;
  line-height: 0.95;
  color: var(--color-text-primary);
}

.match-score-display__detail {
  max-width: 15ch;
  font-size: 0.74rem;
  line-height: 1.35;
  color: var(--color-text-secondary);
  text-wrap: balance;
}

.match-score-display--compact {
  min-width: min(100%, 6.6rem);
  max-width: min(100%, 7.9rem);
  box-shadow: none;
}

.match-score-display--compact::before {
  inset: 0.46rem 0.55rem auto auto;
  width: 1.35rem;
  height: 1.35rem;
  filter: blur(10px);
  opacity: 0.4;
}

.match-score-display--compact .match-score-display__inner {
  min-height: 0;
  padding: 0.52rem 0.62rem 0.58rem;
  gap: 0.14rem;
}

.match-score-display--compact .match-score-display__eyebrow {
  font-size: 0.62rem;
}

.match-score-display--compact .match-score-display__value {
  font-size: clamp(1.18rem, 2.4vw, 1.48rem);
}

.match-score-display--compact .match-score-display__detail {
  max-width: 14ch;
  font-size: 0.68rem;
  line-height: 1.28;
}

.match-score-display--muted .match-score-display__value {
  max-width: 8ch;
  font-size: 0.88rem;
  letter-spacing: -0.01em;
  line-height: 1.16;
  text-wrap: balance;
}

.match-score-display--compact.match-score-display--muted .match-score-display__value {
  font-size: 0.82rem;
}

@media (max-width: 720px) {
  .match-score-display {
    min-width: 0;
    max-width: none;
  }

  .match-score-display__inner {
    min-height: 0;
  }
}
</style>
