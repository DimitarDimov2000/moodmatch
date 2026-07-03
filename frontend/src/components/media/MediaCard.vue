<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';

import TagChip from '@/components/tags/TagChip.vue';
import type { MediaResponse } from '@/types/api';
import {
  canBeFavourite,
  commitmentLevelLabels,
  consumptionStatusLabels,
  mediaTypeLabels,
  metadataOriginLabels,
  sourceTypeLabels,
} from '@/components/media/media-options';

const props = defineProps<{
  media: MediaResponse;
}>();

const visibleTags = computed(() => props.media.tags.slice(0, 5));
const hiddenTagCount = computed(() => Math.max(props.media.tags.length - visibleTags.value.length, 0));
const sourceSummary = computed(() => {
  const sourceParts = [sourceTypeLabels[props.media.sourceType]];

  if (props.media.externalSourceName) {
    sourceParts.push(props.media.externalSourceName.split('_').join(' '));
  }

  if (props.media.metadataOrigin) {
    sourceParts.push(metadataOriginLabels[props.media.metadataOrigin]);
  }

  return sourceParts.join(' · ');
});
</script>

<template>
  <article class="media-card page-card">
    <div class="media-card__body">
      <div class="media-card__aside">
        <img
          v-if="media.coverUrl"
          :src="media.coverUrl"
          :alt="`Cover von ${media.title}`"
          class="media-card__cover"
        >
        <div
          v-else
          class="media-card__cover media-card__cover--placeholder"
        >
          {{ media.title.slice(0, 1).toUpperCase() }}
        </div>

        <div class="media-card__actions">
          <RouterLink
            :to="{ name: 'media-detail', params: { id: media.id } }"
            class="button button--secondary"
          >
            Details
          </RouterLink>
          <span
            v-if="!canBeFavourite(media.consumptionStatus, media.rating)"
            class="media-card__note"
          >
            Favorit erst ab konsumiert und Bewertung 4+
          </span>
        </div>
      </div>

      <div class="media-card__copy">
        <div class="media-card__eyebrow-row">
          <span class="badge badge--accent">
            {{ mediaTypeLabels[media.mediaType] }}
          </span>
          <span class="badge">
            {{ consumptionStatusLabels[media.consumptionStatus] }}
          </span>
          <span
            v-if="media.isFavourite"
            class="badge badge--success"
          >
            Favorit
          </span>
        </div>

        <div class="media-card__header">
          <h2 class="media-card__title">
            {{ media.title }}
          </h2>
        </div>

        <p class="media-card__meta">
          Aufwand: {{ commitmentLevelLabels[media.commitmentLevel] }}
          <span v-if="media.releaseYear"> · {{ media.releaseYear }}</span>
        </p>

        <p
          v-if="media.description"
          class="media-card__description"
        >
          {{ media.description }}
        </p>

        <p class="media-card__source">
          {{ sourceSummary }}
        </p>

        <dl class="media-card__facts">
          <div>
            <dt>Bewertung</dt>
            <dd>{{ media.rating ?? 'Keine' }}</dd>
          </div>
          <div>
            <dt>Umfang</dt>
            <dd>{{ commitmentLevelLabels[media.commitmentLevel] }}</dd>
          </div>
          <div>
            <dt>Tags bestaetigt</dt>
            <dd>{{ media.tags.length }}</dd>
          </div>
        </dl>

        <div
          v-if="visibleTags.length > 0"
          class="media-card__tag-group"
        >
          <p class="media-card__label">
            Zugeordnete Tags
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
              +{{ hiddenTagCount }} weitere
            </span>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.media-card {
  padding: clamp(1.1rem, 2.6vw, 1.35rem);
}

.media-card__body {
  display: grid;
  grid-template-columns: 10.5rem minmax(0, 1fr);
  gap: 1.35rem;
  align-items: start;
}

.media-card__copy {
  display: grid;
  gap: 0.9rem;
}

.media-card__eyebrow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.media-card__header {
  display: grid;
  gap: 0.25rem;
}

.media-card__title {
  margin: 0;
  font-size: clamp(1.2rem, 2.4vw, 1.42rem);
  letter-spacing: -0.02em;
}

.media-card__meta,
.media-card__source,
.media-card__description,
.media-card__note {
  margin: 0;
  color: var(--color-text-secondary);
}

.media-card__source {
  font-size: 0.93rem;
}

.media-card__facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.media-card__facts div {
  padding: 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-surface-secondary);
}

.media-card__facts dt {
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.media-card__facts dd {
  margin: 0.3rem 0 0;
  font-weight: 600;
}

.media-card__aside {
  display: grid;
  gap: 0.95rem;
  justify-items: stretch;
}

.media-card__cover {
  width: 100%;
  aspect-ratio: 4 / 5;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent),
    var(--color-surface-muted);
  box-shadow: var(--shadow-card);
}

.media-card__cover--placeholder {
  display: grid;
  place-items: center;
  color: var(--color-text-muted);
  font-size: 2rem;
  font-weight: 700;
}

.media-card__actions {
  display: grid;
  gap: 0.75rem;
}

.media-card__tag-group {
  display: grid;
  gap: 0.55rem;
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
  gap: 0.65rem;
}

@media (max-width: 820px) {
  .media-card__body {
    grid-template-columns: 1fr;
  }

  .media-card__aside {
    grid-template-columns: 6.5rem 1fr;
    align-items: start;
  }
}

@media (max-width: 560px) {
  .media-card__facts {
    grid-template-columns: 1fr;
  }
}
</style>
