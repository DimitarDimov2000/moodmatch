<script setup lang="ts">
import { computed } from "vue";

import TagChip from "@/components/tags/TagChip.vue";
import type { MatchResultResponse } from "@/types/api";
import { formatDecimal } from "./matching-format";

const props = defineProps<{
  result: MatchResultResponse;
}>();

const rawScoreLabel = computed(() => formatDecimal(props.result.rawScore));
const adjustedScoreLabel = computed(() =>
  formatDecimal(props.result.adjustedScore),
);
const precisionFactorLabel = computed(() =>
  formatDecimal(props.result.precisionFactor),
);
const stateToneClass = computed(() => {
  if (!props.result.candidate.isCompleteForMatching) {
    return "match-explanation__state--warning";
  }

  if (props.result.relativeScore === null) {
    return "match-explanation__state--muted";
  }

  return "match-explanation__state--success";
});
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
    </div>

    <div
      class="match-explanation__state"
      :class="stateToneClass"
    >
      {{
        result.candidate.isCompleteForMatching
          ? "Score-Zustand erklaert"
          : "Explizit unvollstaendig fuer Matching"
      }}
    </div>

    <div class="match-explanation__notes">
      <p class="match-explanation__note">
        {{ result.candidateTagsNote }}
      </p>
      <p
        v-if="result.relativeScore === null"
        class="match-explanation__note"
      >
        Keine Prozentangabe bedeutet hier nicht 0 %, sondern bewusst fehlende
        Vergleichbarkeit.
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
        <dd>{{ rawScoreLabel ?? "Nicht verfuegbar" }}</dd>
      </div>
      <div>
        <dt>Precision</dt>
        <dd>{{ precisionFactorLabel ?? "Nicht verfuegbar" }}</dd>
      </div>
      <div>
        <dt>Adjusted Score</dt>
        <dd>{{ adjustedScoreLabel ?? "Nicht verfuegbar" }}</dd>
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
.match-explanation__notes,
.match-explanation__section {
  display: grid;
  gap: 0.5rem;
}

.match-explanation__copy p,
.match-explanation__label {
  margin: 0;
}

.match-explanation__state {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 0.9rem;
  font-weight: 600;
}

.match-explanation__state--success {
  background: var(--color-success-soft);
  border-color: color-mix(
    in srgb,
    var(--color-success) 30%,
    var(--color-border)
  );
  color: var(--color-success);
}

.match-explanation__state--warning {
  background: var(--color-warning-soft);
  border-color: color-mix(
    in srgb,
    var(--color-warning) 30%,
    var(--color-border)
  );
  color: var(--color-warning);
}

.match-explanation__state--muted {
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
}

.match-explanation__note {
  color: var(--color-text-muted);
  font-size: 0.92rem;
  margin: 0;
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
