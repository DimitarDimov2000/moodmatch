<script setup lang="ts">
import { computed } from "vue";

import TagChip from "@/components/tags/TagChip.vue";
import type { CandidateMediaResponse } from "@/types/api";
import {
  commitmentLevelLabels,
  consumptionStatusLabels,
  mediaTypeLabels,
} from "./matching-format";

const props = withDefaults(
  defineProps<{
    candidate: CandidateMediaResponse;
    title?: string;
    showExpectedNote?: boolean;
    compact?: boolean;
    maxVisibleTags?: number;
    helperText?: string;
  }>(),
  {
    title: "Kandidat",
    showExpectedNote: true,
    compact: false,
    maxVisibleTags: 4,
    helperText: undefined,
  },
);

const statusToneClass = computed(() =>
  props.candidate.isCompleteForMatching
    ? "candidate-summary-card__status--ready"
    : "candidate-summary-card__status--warning",
);

const statusLabel = computed(() =>
  props.candidate.isCompleteForMatching
    ? "Matching bereit"
    : "Tags fehlen fuer Matching",
);

const helperCopy = computed(
  () =>
    props.helperText ??
    (props.candidate.isCompleteForMatching
      ? "Dieser Kandidat hat genug erwartete Tags fuer einen sinnvollen Vergleich."
      : "Ergaenze erwartete Tags, damit MoodMatch diese Option sauber vergleichen kann."),
);
const visibleTags = computed(() =>
  props.candidate.media.tags.slice(0, props.maxVisibleTags),
);
const hiddenTagCount = computed(() =>
  Math.max(props.candidate.media.tags.length - visibleTags.value.length, 0),
);
const metaLine = computed(() => {
  const parts = [
    mediaTypeLabels[props.candidate.media.mediaType],
    consumptionStatusLabels[props.candidate.media.consumptionStatus],
    commitmentLevelLabels[props.candidate.media.commitmentLevel],
  ];

  if (props.candidate.media.releaseYear) {
    parts.push(String(props.candidate.media.releaseYear));
  }

  return parts.join(" · ");
});
</script>

<template>
  <article
    class="candidate-summary-card page-card"
    :class="{ 'candidate-summary-card--compact': compact }"
  >
    <div class="candidate-summary-card__media">
      <img
        v-if="candidate.media.coverUrl"
        :src="candidate.media.coverUrl"
        :alt="`Cover von ${candidate.media.title}`"
        class="candidate-summary-card__cover"
      >
      <div
        v-else
        class="candidate-summary-card__cover candidate-summary-card__cover--placeholder"
      >
        {{ candidate.media.title.slice(0, 1).toUpperCase() }}
      </div>

      <div class="candidate-summary-card__copy">
        <div class="candidate-summary-card__header">
          <div>
            <p class="eyebrow">
              {{ title }}
            </p>
            <h3 class="candidate-summary-card__title">
              {{ candidate.media.title }}
            </h3>
            <p class="candidate-summary-card__meta">
              {{ metaLine }}
            </p>
          </div>

          <span
            class="candidate-summary-card__status"
            :class="statusToneClass"
          >
            {{ statusLabel }}
          </span>
        </div>

        <p class="candidate-summary-card__copy-line">
          {{ helperCopy }}
        </p>

        <div class="candidate-summary-card__facts">
          <span class="candidate-summary-card__fact">
            {{ candidate.media.tags.length }} erwartete Tags
          </span>
          <span class="candidate-summary-card__fact">
            {{
              candidate.isCompleteForMatching
                ? "Vergleich bereit"
                : "Noch unvollstaendig"
            }}
          </span>
        </div>
      </div>
    </div>

    <p
      v-if="showExpectedNote"
      class="candidate-summary-card__note"
    >
      Kandidaten-Tags sind erwartete Merkmale und basieren auf deiner
      Einschaetzung.
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
        v-for="tag in visibleTags"
        :key="tag.id"
        :tag="tag"
      />
      <span
        v-if="hiddenTagCount > 0"
        class="badge"
      >
        +{{ hiddenTagCount }} weitere
      </span>
    </div>
  </article>
</template>

<style scoped>
.candidate-summary-card {
  display: grid;
  gap: 0.9rem;
  padding: clamp(1rem, 2.5vw, 1.2rem);
}

.candidate-summary-card--compact {
  gap: 0.75rem;
}

.candidate-summary-card__media {
  display: grid;
  grid-template-columns: 6rem minmax(0, 1fr);
  gap: 0.9rem;
  align-items: start;
}

.candidate-summary-card__cover {
  width: 100%;
  aspect-ratio: 4 / 5;
  object-fit: cover;
  border-radius: calc(var(--radius-lg) - 6px);
  border: 1px solid var(--color-border);
  background: var(--color-surface-muted);
}

.candidate-summary-card__cover--placeholder {
  display: grid;
  place-items: center;
  font-size: 1.9rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.candidate-summary-card__copy {
  display: grid;
  gap: 0.65rem;
}

.candidate-summary-card__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.85rem;
}

.candidate-summary-card__title,
.candidate-summary-card__meta,
.candidate-summary-card__copy-line,
.candidate-summary-card__note,
.candidate-summary-card__empty {
  margin: 0;
}

.candidate-summary-card__title {
  margin-top: 0.3rem;
  font-size: clamp(1.1rem, 1.8vw, 1.22rem);
}

.candidate-summary-card__meta {
  margin-top: 0.15rem;
  color: var(--color-text-secondary);
}

.candidate-summary-card__copy-line,
.candidate-summary-card__note {
  color: var(--color-text-secondary);
}

.candidate-summary-card__note,
.candidate-summary-card__empty {
  font-size: 0.92rem;
}

.candidate-summary-card__facts {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.candidate-summary-card__fact {
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

.candidate-summary-card__status {
  display: inline-flex;
  align-items: center;
  min-height: 1.85rem;
  padding: 0.24rem 0.68rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  font-size: 0.84rem;
  font-weight: 600;
}

.candidate-summary-card__status--ready {
  background: var(--color-success-soft);
  color: var(--color-success);
  border-color: color-mix(
    in srgb,
    var(--color-success) 30%,
    var(--color-border)
  );
}

.candidate-summary-card__status--warning {
  background: var(--color-warning-soft);
  color: var(--color-warning);
  border-color: color-mix(
    in srgb,
    var(--color-warning) 30%,
    var(--color-border)
  );
}

.candidate-summary-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

@media (max-width: 720px) {
  .candidate-summary-card__media {
    grid-template-columns: 1fr;
  }

  .candidate-summary-card__cover {
    max-width: 7rem;
  }
}
</style>
