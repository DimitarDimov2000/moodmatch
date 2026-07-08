<script setup lang="ts">
import { computed } from "vue";

import MediaArtwork from "@/components/media/MediaArtwork.vue";
import TagChip from "@/components/tags/TagChip.vue";
import { i18n } from "@/i18n";
import type { CandidateMediaResponse } from "@/types/api";
import {
  getMatchingCommitmentLevelLabel,
  getMatchingConsumptionStatusLabel,
  getMatchingMediaTypeLabel,
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
    title: undefined,
    showExpectedNote: true,
    compact: false,
    maxVisibleTags: 4,
    helperText: undefined,
  },
);
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const statusToneClass = computed(() =>
  props.candidate.isCompleteForMatching
    ? "candidate-summary-card__status--ready"
    : "candidate-summary-card__status--warning",
);

const statusLabel = computed(() => {
  trackLocaleDependency();
  return props.candidate.isCompleteForMatching
    ? t("candidateCard.ready")
    : t("candidateCard.missing");
});

const helperCopy = computed(() => {
  trackLocaleDependency();
  return props.helperText ??
    (props.candidate.isCompleteForMatching
      ? t("candidateCard.helperReady")
      : t("candidateCard.helperMissing"));
});
const visibleTags = computed(() =>
  props.candidate.media.tags.slice(0, props.maxVisibleTags),
);
const hiddenTagCount = computed(() =>
  Math.max(props.candidate.media.tags.length - visibleTags.value.length, 0),
);
const metaLine = computed(() => {
  trackLocaleDependency();

  const parts = [
    getMatchingMediaTypeLabel(props.candidate.media.mediaType),
    getMatchingConsumptionStatusLabel(props.candidate.media.consumptionStatus),
    getMatchingCommitmentLevelLabel(props.candidate.media.commitmentLevel),
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
      <MediaArtwork
        class="candidate-summary-card__cover"
        :title="candidate.media.title"
        :media-type="candidate.media.mediaType"
        :cover-url="candidate.media.coverUrl"
      />

      <div class="candidate-summary-card__copy">
        <div class="candidate-summary-card__header">
          <div>
            <p class="eyebrow">
              {{ title ?? t('candidateCard.defaultTitle') }}
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
            {{ t('candidateCard.expectedTagCount', { count: candidate.media.tags.length }) }}
          </span>
          <span class="candidate-summary-card__fact">
            {{
              candidate.isCompleteForMatching
                ? t("candidateCard.compareReady")
                : t("candidateCard.notComplete")
            }}
          </span>
        </div>
      </div>
    </div>

    <p
      v-if="showExpectedNote"
      class="candidate-summary-card__note"
    >
      {{ t("candidateCard.expectedNote") }}
    </p>

    <p
      v-if="candidate.media.tags.length === 0"
      class="candidate-summary-card__empty"
    >
      {{ t("candidateCard.empty") }}
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
        {{ t('matching.moreTags', { count: hiddenTagCount }) }}
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
  border-radius: calc(var(--radius-lg) - 6px);
  overflow: hidden;
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

  .candidate-summary-card__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.55rem;
  }

  .candidate-summary-card__status {
    min-height: 1.7rem;
    padding: 0.2rem 0.58rem;
    font-size: 0.8rem;
  }
}
</style>
