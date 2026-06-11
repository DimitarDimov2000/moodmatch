<script setup lang="ts">
import { RouterLink } from 'vue-router';

import type { MediaResponse } from '@/types/api';
import {
  canBeFavourite,
  commitmentLevelLabels,
  getMediaSubtitle,
} from '@/components/media/media-options';

defineProps<{
  media: MediaResponse;
}>();
</script>

<template>
  <article class="media-card page-card">
    <div class="media-card__body">
      <div class="media-card__copy">
        <div class="media-card__header">
          <h2 class="media-card__title">
            {{ media.title }}
          </h2>
          <span
            v-if="media.isFavourite"
            class="media-card__badge media-card__badge--favourite"
          >
            Favorit
          </span>
        </div>

        <p class="media-card__meta">
          {{ getMediaSubtitle(media) }}
        </p>

        <p
          v-if="media.description"
          class="media-card__description"
        >
          {{ media.description }}
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
            <dt>Tags</dt>
            <dd>{{ media.tags.length }}</dd>
          </div>
        </dl>
      </div>

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
    </div>
  </article>
</template>

<style scoped>
.media-card {
  padding: 1.25rem;
}

.media-card__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 11rem;
  gap: 1.25rem;
}

.media-card__copy {
  display: grid;
  gap: 0.875rem;
}

.media-card__header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.media-card__title {
  margin: 0;
  font-size: 1.35rem;
}

.media-card__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.7rem;
  border-radius: var(--radius-full);
  font-size: 0.8rem;
  font-weight: 600;
}

.media-card__badge--favourite {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.media-card__meta,
.media-card__description,
.media-card__note {
  margin: 0;
  color: var(--color-text-secondary);
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
  gap: 0.875rem;
  justify-items: stretch;
}

.media-card__cover {
  width: 100%;
  aspect-ratio: 4 / 5;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-muted);
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

@media (max-width: 820px) {
  .media-card__body {
    grid-template-columns: 1fr;
  }

  .media-card__aside {
    grid-template-columns: 6rem 1fr;
    align-items: start;
  }
}

@media (max-width: 560px) {
  .media-card__facts {
    grid-template-columns: 1fr;
  }
}
</style>

