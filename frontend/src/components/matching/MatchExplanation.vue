<script setup lang="ts">
import { computed } from "vue";

import { i18n } from "@/i18n";
import TagChip from "@/components/tags/TagChip.vue";
import type { MatchResultResponse } from "@/types/api";
import { formatDecimal } from "./matching-format";
import {
  getGeneratedCandidateTagNote,
  getGeneratedMatchExplanation,
} from "./matching-copy";

const props = withDefaults(
  defineProps<{
    result: MatchResultResponse;
    showHeading?: boolean;
  }>(),
  {
    showHeading: true,
  },
);
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

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
const explanationCopy = computed(() => {
  trackLocaleDependency();
  return getGeneratedMatchExplanation(props.result);
});
const candidateTagNote = computed(() => {
  trackLocaleDependency();
  return getGeneratedCandidateTagNote();
});
</script>

<template>
  <section
    class="match-explanation"
    :class="{ 'match-explanation--compact': !showHeading }"
  >
    <div class="match-explanation__top">
      <div class="match-explanation__copy">
        <h3
          v-if="showHeading"
          class="section-title"
        >
          {{ t("matching.explanationTitle") }}
        </h3>
        <p class="match-explanation__summary-copy body-muted">
          {{ explanationCopy }}
        </p>
      </div>

      <div
        class="match-explanation__state"
        :class="stateToneClass"
      >
        {{
          result.candidate.isCompleteForMatching
            ? result.relativeScore === null
              ? t("matching.cautious")
              : t("matching.understandable")
            : t("matching.incomplete")
        }}
      </div>
    </div>

    <div class="match-explanation__facts">
      <span class="match-explanation__fact">
        {{ t("matching.commonTags", { count: result.matchingTagCount }) }}
      </span>
      <span
        v-if="showHeading"
        class="match-explanation__fact"
      >
        {{ t("matching.expectedTags", { count: result.candidateTagCount }) }}
      </span>
    </div>

    <div
      v-if="result.matchingTags.length"
      class="match-explanation__section"
    >
      <p class="match-explanation__label">
        {{ t("matching.overlappingTags") }}
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
          {{ t('matching.moreTags', { count: hiddenMatchingTagCount }) }}
        </span>
      </div>
    </div>

    <div
      v-if="result.extraCandidateTags.length"
      class="match-explanation__section"
    >
      <p class="match-explanation__label">
        {{ t("matching.extraTags") }}
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
          {{ t('matching.moreTags', { count: hiddenExtraTagCount }) }}
        </span>
      </div>
    </div>

    <details class="match-explanation__details">
      <summary class="match-explanation__summary">
        {{ t("matching.scoreBackground") }}
      </summary>
      <div class="match-explanation__notes">
        <p class="match-explanation__note">
          {{ candidateTagNote }}
        </p>
        <p
          v-if="result.relativeScore === null"
          class="match-explanation__note"
        >
          {{ t("matching.noPercentNotZero") }}
        </p>
      </div>
      <dl class="match-explanation__metrics">
        <div>
          <dt>{{ t("matching.profileHits") }}</dt>
          <dd>
            {{ result.matchingTagCount }} / {{ result.candidateTagCount }} {{ t('tags.title') }}
          </dd>
        </div>
        <div>
          <dt>{{ t("matching.rawScore") }}</dt>
          <dd>{{ rawScoreLabel ?? t("common.states.notAvailable") }}</dd>
        </div>
        <div>
          <dt>{{ t("matching.confidence") }}</dt>
          <dd>{{ precisionFactorLabel ?? t("common.states.notAvailable") }}</dd>
        </div>
        <div>
          <dt>{{ t("matching.finalScore") }}</dt>
          <dd>{{ adjustedScoreLabel ?? t("common.states.notAvailable") }}</dd>
        </div>
      </dl>
    </details>
  </section>
</template>

<style scoped>
.match-explanation {
  display: grid;
  gap: 0.72rem;
}

.match-explanation__top,
.match-explanation__copy,
.match-explanation__notes,
.match-explanation__section {
  display: grid;
  gap: 0.35rem;
}

.match-explanation__top {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65rem;
}

.match-explanation__copy p,
.match-explanation__label {
  margin: 0;
}

.match-explanation__summary-copy {
  line-height: 1.45;
}

.match-explanation--compact .match-explanation__summary-copy {
  font-size: 0.93rem;
}

.match-explanation__facts {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.match-explanation__fact {
  display: inline-flex;
  align-items: center;
  min-height: 1.62rem;
  padding: 0.18rem 0.56rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
}

.match-explanation__state {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 1.7rem;
  padding: 0.2rem 0.6rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 0.8rem;
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
  gap: 0.45rem;
}

.match-explanation__chips > *,
.match-explanation__facts > * {
  max-width: 100%;
}

.match-explanation__chips :deep(.tag-chip) {
  max-width: 100%;
  overflow-wrap: anywhere;
  white-space: normal;
}

.match-explanation__metrics {
  display: grid;
  gap: 0.55rem;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin: 0;
}

.match-explanation__details {
  display: grid;
  gap: 0.6rem;
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
  padding: 0.68rem 0.82rem;
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
