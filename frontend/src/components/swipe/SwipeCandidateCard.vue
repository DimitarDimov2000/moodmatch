<script setup lang="ts">
import { computed } from 'vue';

import MatchScoreDisplay from '@/components/matching/MatchScoreDisplay.vue';
import {
  commitmentLevelLabels,
  mediaTypeLabels,
} from '@/components/matching/matching-format';
import TagChip from '@/components/tags/TagChip.vue';
import type { SwipeQueueItem } from '@/types/swipe';

const props = defineProps<{
  item: SwipeQueueItem;
  matchInsightsAvailable: boolean;
}>();

const readinessLabel = computed(() =>
  props.item.candidate.isCompleteForMatching ? 'Matching bereit' : 'Noch nicht matching-bereit',
);
const readinessToneClass = computed(() =>
  props.item.candidate.isCompleteForMatching
    ? 'swipe-candidate-card__state--ready'
    : 'swipe-candidate-card__state--warning',
);
const scoreStateLabel = computed(() => {
  if (!props.matchInsightsAvailable) {
    return 'Match-Hinweise derzeit nicht verfuegbar';
  }

  if (!props.item.match) {
    return 'Noch kein Match-Ergebnis vorhanden';
  }

  if (props.item.match.relativeScore === null) {
    return 'Score vorhanden, aber bewusst ohne Prozent';
  }

  return 'Relativer Match-Score verfuegbar';
});
const scoreDetail = computed(() => {
  if (!props.matchInsightsAvailable) {
    return 'Du kannst trotzdem lokal durch die Kandidaten gehen.';
  }

  if (!props.item.match) {
    return 'Sobald ein Match vorliegt, erscheint hier die kurze Einordnung.';
  }

  return props.item.match.explanationMessage;
});
</script>

<template>
  <article class="swipe-candidate-card page-card">
    <div class="swipe-candidate-card__hero">
      <div class="swipe-candidate-card__copy">
        <p class="eyebrow">
          Aktuelle Karte
        </p>
        <h2 class="swipe-candidate-card__title">
          {{ item.candidate.media.title }}
        </h2>
        <p class="swipe-candidate-card__meta">
          {{ mediaTypeLabels[item.candidate.media.mediaType] }} ·
          {{ commitmentLevelLabels[item.candidate.media.commitmentLevel] }}
          <span v-if="item.candidate.media.releaseYear">
            · {{ item.candidate.media.releaseYear }}
          </span>
        </p>

        <div class="swipe-candidate-card__states">
          <span
            class="swipe-candidate-card__state"
            :class="readinessToneClass"
          >
            {{ readinessLabel }}
          </span>
          <span class="swipe-candidate-card__state swipe-candidate-card__state--neutral">
            {{ scoreStateLabel }}
          </span>
        </div>
      </div>

      <MatchScoreDisplay
        :score="item.match?.relativeScore ?? null"
        :suppressed="!matchInsightsAvailable"
        insufficient-label="Keine Prozentangabe verfuegbar."
      />
    </div>

    <div class="swipe-candidate-card__grid">
      <section class="swipe-candidate-card__panel">
        <p class="swipe-candidate-card__label">
          Erwartete Tags
        </p>
        <p
          v-if="item.candidate.media.tags.length === 0"
          class="swipe-candidate-card__empty"
        >
          Fuer diesen Kandidaten wurden noch keine erwarteten Tags hinterlegt.
        </p>
        <div
          v-else
          class="swipe-candidate-card__tags"
        >
          <TagChip
            v-for="tag in item.candidate.media.tags"
            :key="tag.id"
            :tag="tag"
          />
        </div>
        <p class="swipe-candidate-card__hint">
          Kandidaten-Tags basieren auf deiner Einschaetzung.
        </p>
      </section>

      <section class="swipe-candidate-card__panel swipe-candidate-card__panel--accent">
        <p class="swipe-candidate-card__label">
          Match-Einordnung
        </p>
        <p class="swipe-candidate-card__detail">
          {{ scoreDetail }}
        </p>
        <p
          v-if="item.match"
          class="swipe-candidate-card__hint"
        >
          {{ item.match.candidateTagsNote }}
        </p>
      </section>
    </div>
  </article>
</template>

<style scoped>
.swipe-candidate-card {
  display: grid;
  gap: 1.5rem;
  padding: 1.5rem;
  background:
    radial-gradient(
      circle at top left,
      color-mix(in srgb, var(--color-accent-soft) 78%, transparent),
      transparent 42%
    ),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--color-surface-secondary) 78%, var(--color-surface)),
      var(--color-surface)
    );
  box-shadow: var(--shadow-swipe-card);
}

.swipe-candidate-card__hero {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.42fr);
  align-items: start;
}

.swipe-candidate-card__copy {
  display: grid;
  gap: 0.65rem;
}

.swipe-candidate-card__title,
.swipe-candidate-card__meta,
.swipe-candidate-card__label,
.swipe-candidate-card__detail,
.swipe-candidate-card__hint,
.swipe-candidate-card__empty {
  margin: 0;
}

.swipe-candidate-card__title {
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 0.98;
}

.swipe-candidate-card__meta,
.swipe-candidate-card__hint,
.swipe-candidate-card__empty {
  color: var(--color-text-secondary);
}

.swipe-candidate-card__states {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.swipe-candidate-card__state {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 0.92rem;
  font-weight: 600;
}

.swipe-candidate-card__state--ready {
  background: var(--color-success-soft);
  border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  color: var(--color-success);
}

.swipe-candidate-card__state--warning {
  background: var(--color-warning-soft);
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
  color: var(--color-warning);
}

.swipe-candidate-card__state--neutral {
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
}

.swipe-candidate-card__grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.swipe-candidate-card__panel {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface) 70%, transparent);
}

.swipe-candidate-card__panel--accent {
  background: color-mix(in srgb, var(--color-info-soft) 42%, var(--color-surface));
}

.swipe-candidate-card__label {
  font-size: 0.9rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--color-text-secondary);
}

.swipe-candidate-card__detail {
  font-size: 1rem;
}

.swipe-candidate-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

@media (max-width: 900px) {
  .swipe-candidate-card__hero,
  .swipe-candidate-card__grid {
    grid-template-columns: 1fr;
  }
}
</style>
