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
const visibleMatchingTags = computed(() =>
  props.result.matchingTags.slice(0, 4),
);
const hiddenMatchingTagCount = computed(() =>
  Math.max(
    props.result.matchingTags.length - visibleMatchingTags.value.length,
    0,
  ),
);
const visibleExtraTags = computed(() =>
  props.result.extraCandidateTags.slice(0, 4),
);
const hiddenExtraTagCount = computed(() =>
  Math.max(
    props.result.extraCandidateTags.length - visibleExtraTags.value.length,
    0,
  ),
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
    <div class="match-explanation__top">
      <div class="match-explanation__copy">
        <h3 class="section-title">
          Warum das passt
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
            ? result.relativeScore === null
              ? "Vergleich noch vorsichtig"
              : "Vergleich gut einordenbar"
            : "Noch unvollstaendig fuer Matching"
        }}
      </div>
    </div>

    <div class="match-explanation__facts">
      <span class="match-explanation__fact">
        {{ result.matchingTagCount }} gemeinsame Tags
      </span>
      <span class="match-explanation__fact">
        {{ result.candidateTagCount }} erwartete Tags
      </span>
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
          v-for="item in visibleMatchingTags"
          :key="item.tag.id"
          :tag="item.tag"
        />
        <span
          v-if="hiddenMatchingTagCount > 0"
          class="badge"
        >
          +{{ hiddenMatchingTagCount }} weitere
        </span>
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
          v-for="tag in visibleExtraTags"
          :key="tag.id"
          :tag="tag"
        />
        <span
          v-if="hiddenExtraTagCount > 0"
          class="badge"
        >
          +{{ hiddenExtraTagCount }} weitere
        </span>
      </div>
    </div>

    <details class="match-explanation__details">
      <summary class="match-explanation__summary">
        Score-Hintergrund ansehen
      </summary>
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
      <dl class="match-explanation__metrics">
        <div>
          <dt>Profiltreffer</dt>
          <dd>
            {{ result.matchingTagCount }} / {{ result.candidateTagCount }} Tags
          </dd>
        </div>
        <div>
          <dt>Rohwert</dt>
          <dd>{{ rawScoreLabel ?? "Nicht verfuegbar" }}</dd>
        </div>
        <div>
          <dt>Vergleichssicherheit</dt>
          <dd>{{ precisionFactorLabel ?? "Nicht verfuegbar" }}</dd>
        </div>
        <div>
          <dt>Berechneter Endwert</dt>
          <dd>{{ adjustedScoreLabel ?? "Nicht verfuegbar" }}</dd>
        </div>
      </dl>
    </details>
  </section>
</template>

<style scoped>
.match-explanation {
  display: grid;
  gap: 0.8rem;
}

.match-explanation__top,
.match-explanation__copy,
.match-explanation__notes,
.match-explanation__section {
  display: grid;
  gap: 0.4rem;
}

.match-explanation__top {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.75rem;
}

.match-explanation__copy p,
.match-explanation__label {
  margin: 0;
}

.match-explanation__facts {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.match-explanation__fact {
  display: inline-flex;
  align-items: center;
  min-height: 1.8rem;
  padding: 0.24rem 0.68rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
}

.match-explanation__state {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 1.85rem;
  padding: 0.24rem 0.68rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 0.84rem;
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
  font-size: 0.88rem;
  margin: 0;
}

.match-explanation__label {
  font-weight: 600;
  color: var(--color-text-primary);
}

.match-explanation__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.match-explanation__metrics {
  display: grid;
  gap: 0.65rem;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin: 0;
}

.match-explanation__details {
  display: grid;
  gap: 0.7rem;
}

.match-explanation__summary {
  color: var(--color-text-secondary);
  font-weight: 700;
  cursor: pointer;
  list-style: none;
}

.match-explanation__summary::-webkit-details-marker {
  display: none;
}

.match-explanation__metrics div {
  padding: 0.75rem 0.9rem;
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

@media (max-width: 720px) {
  .match-explanation__top {
    grid-template-columns: 1fr;
  }
}
</style>
