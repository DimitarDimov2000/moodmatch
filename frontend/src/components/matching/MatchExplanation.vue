<script setup lang="ts">
import { computed } from 'vue';

import TagChip from '@/components/tags/TagChip.vue';
import type { MatchResultResponse } from '@/types/api';
import { formatDecimal } from './matching-format';

const props = defineProps<{
  result: MatchResultResponse;
}>();

const rawScoreLabel = computed(() => formatDecimal(props.result.rawScore));
const adjustedScoreLabel = computed(() => formatDecimal(props.result.adjustedScore));
const precisionFactorLabel = computed(() => formatDecimal(props.result.precisionFactor));
</script>

<template>
  <section class="match-explanation">
    <div class="match-explanation__copy">
      <h3 class="section-title">
        Warum dieser Vorschlag passt
      </h3>
      <p class="body-muted">
        {{ result.explanationMessage }}
      </p>
      <p class="match-explanation__note">
        {{ result.candidateTagsNote }}
      </p>
    </div>

    <div
      v-if="result.matchingTags.length"
      class="match-explanation__section"
    >
      <p class="match-explanation__label">
        Ueberschneidende Tags
      </p>
      <div class="match-explanation__chips">
        <TagChip
          v-for="item in result.matchingTags"
          :key="item.tag.id"
          :tag="item.tag"
        />
      </div>
    </div>

    <div
      v-if="result.extraCandidateTags.length"
      class="match-explanation__section"
    >
      <p class="match-explanation__label">
        Erwartete Tags ohne Profiltreffer
      </p>
      <div class="match-explanation__chips">
        <TagChip
          v-for="tag in result.extraCandidateTags"
          :key="tag.id"
          :tag="tag"
        />
      </div>
    </div>

    <dl class="match-explanation__metrics">
      <div>
        <dt>Matching-Tags</dt>
        <dd>{{ result.matchingTagCount }} / {{ result.candidateTagCount }}</dd>
      </div>
      <div>
        <dt>Raw Score</dt>
        <dd>{{ rawScoreLabel ?? 'Nicht verfuegbar' }}</dd>
      </div>
      <div>
        <dt>Precision</dt>
        <dd>{{ precisionFactorLabel ?? 'Nicht verfuegbar' }}</dd>
      </div>
      <div>
        <dt>Adjusted Score</dt>
        <dd>{{ adjustedScoreLabel ?? 'Nicht verfuegbar' }}</dd>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.match-explanation {
  display: grid;
  gap: 1rem;
}

.match-explanation__copy,
.match-explanation__section {
  display: grid;
  gap: 0.5rem;
}

.match-explanation__copy p,
.match-explanation__label {
  margin: 0;
}

.match-explanation__note {
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.match-explanation__label {
  font-weight: 600;
  color: var(--color-text-primary);
}

.match-explanation__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.match-explanation__metrics {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin: 0;
}

.match-explanation__metrics div {
  padding: 0.85rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-secondary);
}

.match-explanation__metrics dt {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.84rem;
}

.match-explanation__metrics dd {
  margin: 0.3rem 0 0;
  font-size: 1rem;
  font-weight: 600;
}
</style>
