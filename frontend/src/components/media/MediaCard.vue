<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';

import MediaArtwork from '@/components/media/MediaArtwork.vue';
import { i18n } from '@/i18n';
import {
  getDisplayText,
  getExternalSourceLabel,
} from '@/components/media/media-presentation';
import TagChip from '@/components/tags/TagChip.vue';
import type { MediaResponse } from '@/types/api';
import {
  canBeFavourite,
  getCommitmentLevelLabel,
  getConsumptionStatusLabel,
  getMediaTypeLabel,
  getMetadataOriginLabel,
  getSourceTypeLabel,
} from '@/components/media/media-options';

const props = defineProps<{
  media: MediaResponse;
}>();
const { t } = i18n.global;

const visibleTags = computed(() => props.media.tags.slice(0, 4));
const hiddenTagCount = computed(() =>
  Math.max(props.media.tags.length - visibleTags.value.length, 0),
);
const sourceBadgeLabel = computed(() =>
  props.media.externalSourceName
    ? getExternalSourceLabel(props.media.externalSourceName)
    : getSourceTypeLabel(props.media.sourceType),
);
const subtitle = computed(() =>
  [
    getConsumptionStatusLabel(props.media.consumptionStatus),
    getCommitmentLevelLabel(props.media.commitmentLevel),
    props.media.releaseYear ? String(props.media.releaseYear) : null,
  ].filter((value): value is string => Boolean(value)).join(' • '),
);

const detailBadges = computed(() =>
  [
    props.media.metadataOrigin
      ? getMetadataOriginLabel(props.media.metadataOrigin)
      : null,
  ].filter((value): value is string => Boolean(value)),
);
const ratingLabel = computed(() =>
  props.media.rating === null
    ? t('common.states.noRating')
    : t('mediaCard.ratingOutOfFive', { rating: props.media.rating }),
);
const displayTitle = computed(() => getDisplayText(props.media.title));
const favouriteEligible = computed(() =>
  canBeFavourite(props.media.consumptionStatus, props.media.rating),
);
const descriptionPreview = computed(
  () => getDisplayText(props.media.description).trim(),
);
const tagSummary = computed(() =>
  props.media.tags.length === 1
    ? t('mediaCard.oneTag')
    : t('mediaCard.manyTags', { count: props.media.tags.length }),
);
</script>

<template>
  <article class="media-card page-card">
    <div class="media-card__body">
      <div class="media-card__aside">
        <MediaArtwork
          class="media-card__cover"
          :title="displayTitle"
          :media-type="media.mediaType"
          :cover-url="media.coverUrl"
        />

        <div class="media-card__actions">
          <RouterLink
            :to="{ name: 'media-detail', params: { id: media.id } }"
            class="button button--secondary"
          >
            {{ t('mediaCard.details') }}
          </RouterLink>
          <span
            v-if="!favouriteEligible"
            class="media-card__note"
          >
            {{ t('mediaCard.favouriteRequirement') }}
          </span>
        </div>
      </div>

      <div class="media-card__copy">
        <div class="media-card__badge-row">
          <span class="badge">
            {{ getMediaTypeLabel(media.mediaType) }}
          </span>
          <span class="badge badge--accent">
            {{ sourceBadgeLabel }}
          </span>
          <span
            v-if="media.releaseYear"
            class="badge"
          >
            {{ media.releaseYear }}
          </span>
        </div>

        <div class="media-card__header">
          <h2 class="media-card__title">
            {{ displayTitle }}
          </h2>
          <p
            v-if="subtitle"
            class="media-card__meta"
          >
            {{ subtitle }}
          </p>
        </div>

        <div class="media-card__eyebrow-row">
          <span
            v-for="badge in detailBadges"
            :key="badge"
            class="badge"
          >
            {{ badge }}
          </span>
          <span
            v-if="media.isFavourite"
            class="badge badge--success"
          >
            {{ t('mediaCard.favourite') }}
          </span>
        </div>

        <div class="media-card__facts">
          <span class="media-card__fact">
            {{ ratingLabel }}
          </span>
          <span class="media-card__fact">
            {{ tagSummary }}
          </span>
        </div>

        <p
          v-if="descriptionPreview"
          class="media-card__description"
        >
          {{ descriptionPreview }}
        </p>

        <div
          v-if="visibleTags.length > 0"
          class="media-card__tag-group"
        >
          <p class="media-card__label">
            {{ t('mediaCard.assignedTags') }}
          </p>
          <div class="media-card__tags">
            <TagChip
              v-for="tag in visibleTags"
              :key="tag.id"
              :tag="tag"
            />
            <span
              v-if="hiddenTagCount > 0"
              class="badge"
            >
              {{ t('mediaCard.moreTags', { count: hiddenTagCount }) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.media-card {
  padding: clamp(1rem, 2.4vw, 1.15rem);
}

.media-card__body {
  display: grid;
  grid-template-columns: 8.6rem minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.media-card__copy {
  display: grid;
  gap: 0.7rem;
}

.media-card__badge-row,
.media-card__eyebrow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.media-card__header {
  display: grid;
  gap: 0.15rem;
}

.media-card__title {
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  font-size: clamp(1.12rem, 2vw, 1.3rem);
  letter-spacing: -0.02em;
  line-height: 1.08;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.media-card__meta,
.media-card__description,
.media-card__note {
  margin: 0;
  color: var(--color-text-secondary);
}

.media-card__facts {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.media-card__fact {
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

.media-card__fact--accent {
  background: var(--color-accent-soft);
  border-color: color-mix(
    in srgb,
    var(--color-accent) 28%,
    var(--color-border)
  );
  color: var(--theme-badge-accent-text);
}

.media-card__aside {
  display: grid;
  gap: 0.75rem;
  align-content: start;
  justify-items: stretch;
}

.media-card__actions {
  display: grid;
  gap: 0.6rem;
}

.media-card__tag-group {
  display: grid;
  gap: 0.45rem;
}

.media-card__label {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.media-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.media-card__description {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

@media (max-width: 820px) {
  .media-card__body {
    grid-template-columns: 1fr;
  }

  .media-card__aside {
    grid-template-columns: 6rem 1fr;
    align-items: start;
  }
}
</style>
