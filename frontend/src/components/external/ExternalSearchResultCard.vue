<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';

import { externalSourceLabels } from '@/components/external/external-options';
import { mediaTypeLabels } from '@/components/media/media-options';
import type { ExternalSearchResultResponse } from '@/types/api';

const DESCRIPTION_PREVIEW_LIMIT = 280;

const props = defineProps<{
  result: ExternalSearchResultResponse;
  isImporting?: boolean;
  importError?: string;
  importedMediaId?: string | null;
  importMessage?: string | null;
}>();

const emit = defineEmits<{
  import: [result: ExternalSearchResultResponse];
}>();

const sourceLabel = computed(() => externalSourceLabels[props.result.source]);
const mediaTypeLabel = computed(() => mediaTypeLabels[props.result.mediaType]);
const subtypeLabel = computed(() => {
  const format = props.result.externalSubjects.find((subject) => subject.startsWith('Format: '))?.replace('Format: ', '');
  if (props.result.source === 'OPEN_LIBRARY') {
    return 'Book';
  }
  if (props.result.source === 'LIBRIVOX') {
    return 'Audiobook';
  }
  if (props.result.source === 'PODCAST_INDEX') {
    return 'Podcast show';
  }
  if (props.result.source !== 'ANILIST' || !format) {
    return format ?? '';
  }

  if (props.result.mediaType === 'FILM') {
    return 'Anime movie';
  }
  if (props.result.mediaType === 'SERIES') {
    return 'Anime series';
  }
  if (format === 'NOVEL') {
    return 'Light novel';
  }
  if (format === 'ONE_SHOT') {
    return 'Manga one-shot';
  }
  return 'Manga';
});
const creatorLabel = computed(() => {
  if (props.result.source === 'ANILIST' && props.result.mediaType !== 'BOOK') {
    return 'Studios';
  }
  if (props.result.mediaType === 'BOOK') {
    return 'Autor:innen';
  }
  if (props.result.mediaType === 'AUDIOBOOK') {
    return 'Autor:in / Sprecher:in';
  }
  if (props.result.mediaType === 'PODCAST') {
    return 'Host / Autor:in';
  }
  if (props.result.mediaType === 'GAME') {
    return 'Entwicklung / Publisher';
  }
  if (props.result.mediaType === 'VIDEO') {
    return 'Channel';
  }
  return 'Mitwirkende';
});
const subjectsLabel = computed(() => {
  if (props.result.source === 'ANILIST') {
    return 'Format / Status / Tags';
  }
  if (props.result.mediaType === 'GAME') {
    return 'Platforms / Tags';
  }
  if (props.result.mediaType === 'PODCAST') {
    return 'Sprache / Hinweise';
  }
  if (props.result.mediaType === 'VIDEO') {
    return 'Tags / Kategorie';
  }
  return 'Externe Subjects';
});
const primarySummary = computed(() => {
  const parts = [mediaTypeLabel.value, sourceLabel.value];
  if (subtypeLabel.value) {
    parts.push(subtypeLabel.value);
  }
  return parts.join(' · ');
});
const secondarySummary = computed(() => {
  const parts: string[] = [];
  if (props.result.releaseYear) {
    parts.push(String(props.result.releaseYear));
  }
  return parts.join(' • ');
});
const descriptionPreview = computed(() => trimText(props.result.description, DESCRIPTION_PREVIEW_LIMIT));
const hasTrimmedDescription = computed(
  () =>
    Boolean(props.result.description)
    && descriptionPreview.value.length < (props.result.description?.trim().length ?? 0),
);
const visibleGenres = computed(() => props.result.externalGenres.slice(0, 8));
const visibleSubjects = computed(() => props.result.externalSubjects.slice(0, 10));
const coverFallback = computed(() => fallbackForMediaType(props.result.mediaType));
const coverClass = computed(() => ({
  'external-result-card__cover--video-thumbnail': props.result.mediaType === 'VIDEO',
}));

function trimText(value: string | null, maxLength: number): string {
  const normalized = value?.trim();
  if (!normalized) {
    return '';
  }
  if (normalized.length <= maxLength) {
    return normalized;
  }

  return `${normalized.slice(0, maxLength - 1).trimEnd()}…`;
}

function fallbackForMediaType(mediaType: ExternalSearchResultResponse['mediaType']) {
  switch (mediaType) {
    case 'FILM':
      return { code: 'FILM', label: 'Film', hint: 'Kein Poster' };
    case 'SERIES':
      return { code: 'SERIES', label: 'Serie', hint: 'Kein Serien-Cover' };
    case 'BOOK':
      return { code: 'BOOK', label: 'Buch', hint: 'Kein Buchcover' };
    case 'AUDIOBOOK':
      return { code: 'AUDIO', label: 'Hoerbuch', hint: 'Kein Audiobook-Cover' };
    case 'GAME':
      return { code: 'GAME', label: 'Spiel', hint: 'Kein Key Art' };
    case 'PODCAST':
      return { code: 'POD', label: 'Podcast', hint: 'Kein Podcast-Cover' };
    case 'VIDEO':
      return { code: 'VIDEO', label: 'Video', hint: 'Kein Thumbnail' };
  }
}
</script>

<template>
  <article class="external-result-card page-card">
    <div class="external-result-card__layout">
      <div class="external-result-card__copy">
        <div class="external-result-card__eyebrow-row">
          <span class="eyebrow">External Preview</span>
          <span class="external-result-card__source-badge">{{ sourceLabel }}</span>
          <span class="external-result-card__media-badge">{{ mediaTypeLabel }}</span>
          <span
            v-if="subtypeLabel"
            class="external-result-card__subtype-badge"
          >
            {{ subtypeLabel }}
          </span>
        </div>

        <h2 class="external-result-card__title">
          {{ result.title }}
        </h2>

        <p class="body-muted external-result-card__summary">
          {{ primarySummary }}
        </p>

        <p
          v-if="secondarySummary"
          class="body-muted"
        >
          {{ secondarySummary }}
        </p>

        <p
          v-if="result.originalTitle"
          class="body-muted"
        >
          Originaltitel: {{ result.originalTitle }}
        </p>

        <div
          v-if="result.creatorNames.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            {{ creatorLabel }}
          </p>
          <p class="body-muted">
            {{ result.creatorNames.join(', ') }}
          </p>
        </div>

        <div
          v-if="result.mediaType === 'AUDIOBOOK'"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            Quelle
          </p>
          <p class="body-muted">
            {{ sourceLabel }} Audiobook Catalog
          </p>
        </div>

        <div
          v-if="result.mediaType === 'PODCAST'"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            Quelle
          </p>
          <p class="body-muted">
            {{ sourceLabel }} Podcast Catalog
          </p>
        </div>

        <p
          v-if="descriptionPreview"
          class="external-result-card__description"
          :title="hasTrimmedDescription ? result.description ?? undefined : undefined"
        >
          {{ descriptionPreview }}
        </p>

        <div
          v-if="visibleGenres.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            Externe Genres
          </p>
          <div class="external-result-card__chip-row">
            <span
              v-for="genre in visibleGenres"
              :key="genre"
              class="external-result-card__chip external-result-card__chip--genre"
            >
              {{ genre }}
            </span>
          </div>
        </div>

        <div
          v-if="visibleSubjects.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            {{ subjectsLabel }}
          </p>
          <div class="external-result-card__chip-row">
            <span
              v-for="subject in visibleSubjects"
              :key="subject"
              class="external-result-card__chip external-result-card__chip--subject"
            >
              {{ subject }}
            </span>
          </div>
        </div>

        <div class="external-result-card__section">
          <p class="external-result-card__section-label">
            Vorgeschlagene Tags
          </p>
          <p
            v-if="result.suggestedTags.length === 0"
            class="body-muted"
          >
            Fuer dieses Ergebnis liegen noch keine gemappten Tag-Vorschlaege vor.
          </p>
          <div
            v-else
            class="external-result-card__chip-row"
          >
            <span
              v-for="suggestedTag in result.suggestedTags"
              :key="`${suggestedTag.tagId}-${suggestedTag.sourceValue}`"
              class="external-result-card__chip external-result-card__chip--suggested"
            >
              {{ suggestedTag.tagName }} - {{ suggestedTag.confidence }}
            </span>
          </div>
        </div>

        <div
          v-if="result.warnings.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            Hinweise
          </p>
          <div class="external-result-card__warning-list">
            <p
              v-for="warning in result.warnings"
              :key="warning"
              class="body-muted"
            >
              {{ warning }}
            </p>
          </div>
        </div>

        <footer class="external-result-card__footer">
          <div class="external-result-card__footer-copy">
            <p class="body-muted">
              {{ result.attribution }}
            </p>
            <p
              v-if="importError"
              class="external-result-card__status external-result-card__status--error"
            >
              {{ importError }}
            </p>
            <p
              v-else-if="importMessage"
              class="external-result-card__status external-result-card__status--success"
            >
              {{ importMessage }}
            </p>
          </div>

          <div class="external-result-card__action-row">
            <a
              v-if="result.sourceUrl"
              :href="result.sourceUrl"
              class="external-result-card__link"
              target="_blank"
              rel="noreferrer"
            >
              Detail-Quelle ansehen
            </a>
            <RouterLink
              v-if="importedMediaId"
              :to="{ name: 'media-detail', params: { id: importedMediaId } }"
              class="button button--secondary"
            >
              In Mediathek ansehen
            </RouterLink>
            <button
              v-else
              class="button button--primary"
              type="button"
              :disabled="isImporting"
              @click="emit('import', props.result)"
            >
              {{ isImporting ? 'Import laeuft...' : 'In Mediathek importieren' }}
            </button>
          </div>
        </footer>
      </div>

      <div class="external-result-card__cover-frame">
        <img
          v-if="result.coverUrl"
          :src="result.coverUrl"
          :alt="`Cover von ${result.title}`"
          class="external-result-card__cover"
          :class="coverClass"
          loading="lazy"
          decoding="async"
          :width="result.mediaType === 'VIDEO' ? 224 : 160"
          :height="result.mediaType === 'VIDEO' ? 126 : 200"
        >
        <div
          v-else
          class="external-result-card__cover external-result-card__cover--fallback"
          :class="`external-result-card__cover--${result.mediaType.toLowerCase()}`"
          :aria-label="`${coverFallback.label} Placeholder`"
        >
          <span class="external-result-card__cover-code">{{ coverFallback.code }}</span>
          <span class="external-result-card__cover-label">{{ coverFallback.label }}</span>
          <span class="external-result-card__cover-hint">{{ coverFallback.hint }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.external-result-card {
  padding: 1.5rem;
}

.external-result-card__layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 1fr) 10rem;
}

.external-result-card__copy {
  display: grid;
  gap: 0.9rem;
}

.external-result-card__eyebrow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  align-items: center;
}

.external-result-card__source-badge {
  display: inline-flex;
  padding: 0.35rem 0.7rem;
  border-radius: var(--radius-full);
  background: var(--color-info-soft);
  color: var(--color-info);
  border: 1px solid color-mix(in srgb, var(--color-info) 24%, var(--color-border));
  font-size: 0.85rem;
  font-weight: 600;
}

.external-result-card__media-badge,
.external-result-card__subtype-badge {
  display: inline-flex;
  padding: 0.35rem 0.7rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
  font-size: 0.85rem;
  font-weight: 600;
}

.external-result-card__subtype-badge {
  background: color-mix(in srgb, var(--color-warning-soft) 72%, var(--color-surface));
  color: var(--color-text-primary);
}

.external-result-card__title,
.external-result-card__description,
.external-result-card__section-label {
  margin: 0;
}

.external-result-card__summary {
  font-weight: 600;
}

.external-result-card__description {
  color: var(--color-text-secondary);
  line-height: 1.55;
}

.external-result-card__section {
  display: grid;
  gap: 0.55rem;
}

.external-result-card__section-label {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.external-result-card__chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.external-result-card__chip {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0.7rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.external-result-card__chip--genre {
  background: color-mix(in srgb, var(--color-info-soft) 80%, var(--color-surface));
}

.external-result-card__chip--subject {
  background: color-mix(in srgb, var(--color-warning-soft) 72%, var(--color-surface));
}

.external-result-card__chip--suggested {
  background: color-mix(in srgb, var(--color-success-soft) 78%, var(--color-surface));
  color: var(--color-success);
}

.external-result-card__warning-list {
  display: grid;
  gap: 0.35rem;
}

.external-result-card__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
  justify-content: space-between;
  padding-top: 0.35rem;
}

.external-result-card__footer-copy {
  display: grid;
  gap: 0.35rem;
}

.external-result-card__action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: flex-end;
}

.external-result-card__status {
  margin: 0;
  font-weight: 600;
}

.external-result-card__status--success {
  color: var(--color-success);
}

.external-result-card__status--error {
  color: var(--color-danger);
}

.external-result-card__link {
  color: var(--color-accent-dark);
  font-weight: 600;
}

.external-result-card__cover-frame {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
}

.external-result-card__cover {
  width: 10rem;
  min-width: 10rem;
  aspect-ratio: 4 / 5;
  object-fit: cover;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
}

.external-result-card__cover--video-thumbnail {
  width: 14rem;
  min-width: 14rem;
  aspect-ratio: 16 / 9;
}

.external-result-card__cover--fallback {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 0.35rem;
  padding: 1rem;
  color: var(--color-text-muted);
  text-align: center;
}

.external-result-card__cover-code {
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--color-text-primary);
}

.external-result-card__cover-label,
.external-result-card__cover-hint {
  font-size: 0.9rem;
}

.external-result-card__cover-label {
  font-weight: 700;
  color: var(--color-text-primary);
}

.external-result-card__cover--film {
  background: color-mix(in srgb, #f59e0b 16%, var(--color-surface-secondary));
}

.external-result-card__cover--series {
  background: color-mix(in srgb, #2563eb 14%, var(--color-surface-secondary));
}

.external-result-card__cover--book {
  background: color-mix(in srgb, #16a34a 14%, var(--color-surface-secondary));
}

.external-result-card__cover--audiobook {
  background: color-mix(in srgb, #ef4444 12%, var(--color-surface-secondary));
}

.external-result-card__cover--game {
  background: color-mix(in srgb, #0f766e 14%, var(--color-surface-secondary));
}

.external-result-card__cover--podcast {
  background: color-mix(in srgb, #d97706 14%, var(--color-surface-secondary));
}

.external-result-card__cover--video {
  background: color-mix(in srgb, #9333ea 12%, var(--color-surface-secondary));
}

@media (max-width: 820px) {
  .external-result-card__layout {
    grid-template-columns: 1fr;
  }

  .external-result-card__cover-frame {
    justify-content: flex-start;
  }
}
</style>
