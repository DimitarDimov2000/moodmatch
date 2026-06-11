<script setup lang="ts">
import { computed } from 'vue';

import TagChip from '@/components/tags/TagChip.vue';
import type { CandidateMediaResponse } from '@/types/api';
import {
  commitmentLevelLabels,
  consumptionStatusLabels,
  mediaTypeLabels,
} from './matching-format';

const props = withDefaults(
  defineProps<{
    candidate: CandidateMediaResponse;
    title?: string;
    showExpectedNote?: boolean;
  }>(),
  {
    title: 'Kandidat',
    showExpectedNote: true,
  },
);

const statusToneClass = computed(() =>
  props.candidate.isCompleteForMatching ? 'candidate-summary-card__status--ready' : 'candidate-summary-card__status--warning',
);
</script>

<template>
  <article class="candidate-summary-card page-card">
    <div class="candidate-summary-card__header">
      <div>
        <p class="eyebrow">
          {{ title }}
        </p>
        <h3 class="candidate-summary-card__title">
          {{ candidate.media.title }}
        </h3>
        <p class="candidate-summary-card__meta">
          {{ mediaTypeLabels[candidate.media.mediaType] }} ·
          {{ consumptionStatusLabels[candidate.media.consumptionStatus] }} ·
          {{ commitmentLevelLabels[candidate.media.commitmentLevel] }}
        </p>
      </div>

      <span
        class="candidate-summary-card__status"
        :class="statusToneClass"
      >
        {{ candidate.isCompleteForMatching ? 'Matching bereit' : 'Tags fehlen fuer Matching' }}
      </span>
    </div>

    <p
      v-if="showExpectedNote"
      class="candidate-summary-card__note"
    >
      Kandidaten-Tags sind erwartete Merkmale und basieren auf deiner Einschaetzung.
    </p>

    <p
      v-if="candidate.media.tags.length === 0"
      class="candidate-summary-card__empty"
    >
      Fuer diesen Kandidaten wurden noch keine erwarteten Tags hinterlegt.
    </p>

    <div
      v-else
      class="candidate-summary-card__tags"
    >
      <TagChip
        v-for="tag in candidate.media.tags"
        :key="tag.id"
        :tag="tag"
      />
    </div>
  </article>
</template>

<style scoped>
.candidate-summary-card {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.candidate-summary-card__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
}

.candidate-summary-card__title,
.candidate-summary-card__meta,
.candidate-summary-card__note,
.candidate-summary-card__empty {
  margin: 0;
}

.candidate-summary-card__title {
  margin-top: 0.35rem;
  font-size: 1.2rem;
}

.candidate-summary-card__meta,
.candidate-summary-card__note {
  color: var(--color-text-secondary);
}

.candidate-summary-card__note,
.candidate-summary-card__empty {
  font-size: 0.95rem;
}

.candidate-summary-card__status {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  font-size: 0.9rem;
  font-weight: 600;
}

.candidate-summary-card__status--ready {
  background: var(--color-success-soft);
  color: var(--color-success);
  border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border));
}

.candidate-summary-card__status--warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
}

.candidate-summary-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}
</style>
