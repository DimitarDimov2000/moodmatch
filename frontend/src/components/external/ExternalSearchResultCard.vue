<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';

import MediaArtwork from '@/components/media/MediaArtwork.vue';
import { mediaTypeLabels } from '@/components/media/media-options';
import { getExternalSourceLabel } from '@/components/media/media-presentation';
import type { ExternalSearchResultResponse } from '@/types/api';

const DESCRIPTION_PREVIEW_LIMIT = 220;

const props = defineProps<{
  result: ExternalSearchResultResponse;
  isImporting?: boolean;
  importError?: string;
  importedMediaId?: string | null;
  importMessage?: string | null;
  importCreated?: boolean | null;
}>();

const emit = defineEmits<{
  import: [result: ExternalSearchResultResponse];
}>();

const sourceLabel = computed(() => getExternalSourceLabel(props.result.source));
const mediaTypeLabel = computed(() => mediaTypeLabels[props.result.mediaType]);
const subtypeLabel = computed(() => {
  const format = props.result.externalSubjects.find((subject) => subject.startsWith('Format: '))?.replace('Format: ', '');

  if (props.result.source === 'OPEN_LIBRARY') {
    return 'Buch';
  }

  if (props.result.source === 'LIBRIVOX') {
    return 'Hoerbuch';
  }

  if (props.result.source === 'PODCAST_INDEX') {
    return 'Podcast-Show';
  }

  if (props.result.source !== 'ANILIST' || !format) {
    return '';
  }

  if (props.result.mediaType === 'FILM') {
    return 'Anime-Film';
  }

  if (props.result.mediaType === 'SERIES') {
    return 'Anime-Serie';
  }

  if (format === 'NOVEL') {
    return 'Light Novel';
  }

  if (format === 'ONE_SHOT') {
    return 'One-shot';
  }

  return 'Manga';
});
const creatorLabel = computed(() => {
  if (props.result.source === 'ANILIST' && props.result.mediaType !== 'BOOK') {
    return 'Studio';
  }

  if (props.result.mediaType === 'BOOK') {
    return 'Autor:innen';
  }

  if (props.result.mediaType === 'AUDIOBOOK') {
    return 'Autor:in & Stimme';
  }

  if (props.result.mediaType === 'PODCAST') {
    return 'Host / Creator';
  }

  if (props.result.mediaType === 'GAME') {
    return 'Studio / Publisher';
  }

  if (props.result.mediaType === 'VIDEO') {
    return 'Kanal';
  }

  return 'Mitwirkende';
});
const normalizedSubjects = computed(() =>
  props.result.externalSubjects
    .map((subject) => normalizeSubject(subject))
    .filter((subject): subject is string => Boolean(subject))
    .slice(0, 8),
);
const descriptionPreview = computed(() => trimText(props.result.description, DESCRIPTION_PREVIEW_LIMIT));
const hasTrimmedDescription = computed(
  () =>
    Boolean(props.result.description)
    && descriptionPreview.value.length < (props.result.description?.trim().length ?? 0),
);
const visibleGenres = computed(() => props.result.externalGenres.slice(0, 6));
const visibleSuggestedTags = computed(() => props.result.suggestedTags.slice(0, 6));
const summaryFacts = computed(() =>
  [
    props.result.releaseYear ? `Jahr ${props.result.releaseYear}` : null,
    props.result.originalTitle ? `Originaltitel: ${props.result.originalTitle}` : null,
  ].filter((value): value is string => Boolean(value)),
);
const importStatusBadge = computed(() => {
  if (!props.importMessage || !props.importedMediaId) {
    return null;
  }

  if (props.importCreated === false) {
    return 'Bereits vorhanden';
  }

  return 'Importiert';
});
const artworkVariant = computed(() =>
  props.result.mediaType === 'VIDEO' ? 'landscape' : 'poster',
);

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

function normalizeSubject(subject: string): string | null {
  if (subject.startsWith('Format: ')) {
    return null;
  }

  if (subject.startsWith('Status: ')) {
    return `Status: ${formatStatusValue(subject.replace('Status: ', ''))}`;
  }

  if (subject.startsWith('Language: ')) {
    return `Sprache: ${subject.replace('Language: ', '').toUpperCase()}`;
  }

  if (subject.startsWith('Explicit: ')) {
    return `Explizit: ${subject.replace('Explicit: ', '') === 'No' ? 'Nein' : 'Ja'}`;
  }

  if (subject.startsWith('Feed type: ')) {
    return `Feed: ${toSentenceCase(subject.replace('Feed type: ', ''))}`;
  }

  if (subject.startsWith('Channel: ')) {
    return `Kanal: ${subject.replace('Channel: ', '')}`;
  }

  if (subject.startsWith('Category: ')) {
    return `Kategorie: ${subject.replace('Category: ', '')}`;
  }

  return subject;
}

function formatStatusValue(value: string): string {
  switch (value) {
    case 'FINISHED':
      return 'Abgeschlossen';
    case 'RELEASING':
      return 'Laufend';
    case 'NOT_YET_RELEASED':
      return 'Noch nicht erschienen';
    default:
      return toSentenceCase(value.replace(/_/g, ' '));
  }
}

function toSentenceCase(value: string): string {
  return value
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}
</script>

<template>
  <article class="external-result-card page-card">
    <div class="external-result-card__layout">
      <div class="external-result-card__cover-column">
        <MediaArtwork
          class="external-result-card__cover"
          :title="result.title"
          :media-type="result.mediaType"
          :cover-url="result.coverUrl"
          :variant="artworkVariant"
        />
      </div>

      <div class="external-result-card__copy">
        <div class="external-result-card__header">
          <div class="external-result-card__eyebrow-row">
            <span class="eyebrow">Importvorschau</span>
            <span class="badge badge--accent">{{ sourceLabel }}</span>
            <span class="badge">{{ mediaTypeLabel }}</span>
            <span
              v-if="subtypeLabel"
              class="badge"
            >
              {{ subtypeLabel }}
            </span>
            <span
              v-if="importStatusBadge"
              class="badge badge--success"
            >
              {{ importStatusBadge }}
            </span>
          </div>

          <h2 class="external-result-card__title">
            {{ result.title }}
          </h2>

          <div
            v-if="summaryFacts.length > 0"
            class="external-result-card__fact-row"
          >
            <span
              v-for="fact in summaryFacts"
              :key="fact"
              class="external-result-card__fact"
            >
              {{ fact }}
            </span>
          </div>
        </div>

        <div
          v-if="result.creatorNames.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            {{ creatorLabel }}
          </p>
          <p class="body-muted external-result-card__supporting-copy">
            {{ result.creatorNames.join(', ') }}
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
            Genres
          </p>
          <div class="external-result-card__chip-row">
            <span
              v-for="genre in visibleGenres"
              :key="genre"
              class="badge"
            >
              {{ genre }}
            </span>
          </div>
        </div>

        <div
          v-if="normalizedSubjects.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            Hinweise
          </p>
          <div class="external-result-card__chip-row">
            <span
              v-for="subject in normalizedSubjects"
              :key="subject"
              class="badge"
            >
              {{ subject }}
            </span>
          </div>
        </div>

        <div class="external-result-card__section">
          <p class="external-result-card__section-label">
            Gemappte Tags
          </p>

          <p
            v-if="visibleSuggestedTags.length === 0"
            class="body-muted external-result-card__supporting-copy"
          >
            Noch keine passenden Tag-Vorschlaege vorhanden.
          </p>

          <div
            v-else
            class="external-result-card__chip-row"
          >
            <span
              v-for="suggestedTag in visibleSuggestedTags"
              :key="`${suggestedTag.tagId}-${suggestedTag.sourceValue}`"
              class="badge badge--success"
            >
              {{ suggestedTag.tagName }}
            </span>
          </div>
        </div>

        <div
          v-if="result.warnings.length > 0"
          class="external-result-card__warning-list"
        >
          <p
            v-for="warning in result.warnings"
            :key="warning"
            class="external-result-card__warning"
          >
            {{ warning }}
          </p>
        </div>

        <footer class="external-result-card__footer">
          <div class="external-result-card__footer-copy">
            <p class="external-result-card__attribution-label">
              Provider
            </p>
            <p class="body-muted external-result-card__supporting-copy">
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
              Quelle oeffnen
            </a>
            <RouterLink
              v-if="importedMediaId"
              :to="{ name: 'media-detail', params: { id: importedMediaId } }"
              class="button button--secondary"
            >
              Details
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
    </div>
  </article>
</template>

<style scoped>
.external-result-card {
  padding: clamp(1rem, 2.4vw, 1.2rem);
}

.external-result-card__layout {
  display: grid;
  gap: 1rem 1.15rem;
  grid-template-columns: minmax(0, 11rem) minmax(0, 1fr);
  align-items: start;
}

.external-result-card__cover-column {
  width: 100%;
}

.external-result-card__cover {
  width: 100%;
}

.external-result-card__copy,
.external-result-card__header,
.external-result-card__section,
.external-result-card__footer-copy,
.external-result-card__warning-list {
  display: grid;
}

.external-result-card__copy {
  gap: 0.9rem;
}

.external-result-card__header {
  gap: 0.5rem;
}

.external-result-card__eyebrow-row,
.external-result-card__chip-row,
.external-result-card__fact-row,
.external-result-card__action-row {
  display: flex;
  flex-wrap: wrap;
}

.external-result-card__eyebrow-row {
  gap: 0.45rem;
  align-items: center;
}

.external-result-card__title,
.external-result-card__description,
.external-result-card__section-label,
.external-result-card__warning,
.external-result-card__attribution-label,
.external-result-card__status {
  margin: 0;
}

.external-result-card__title {
  font-size: clamp(1.12rem, 2vw, 1.34rem);
  line-height: 1.1;
  letter-spacing: -0.025em;
}

.external-result-card__fact-row {
  gap: 0.45rem;
}

.external-result-card__fact {
  display: inline-flex;
  align-items: center;
  min-height: 1.8rem;
  padding: 0.25rem 0.72rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
}

.external-result-card__section {
  gap: 0.45rem;
}

.external-result-card__section-label,
.external-result-card__attribution-label {
  color: var(--color-text-muted);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.external-result-card__supporting-copy,
.external-result-card__description {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.55;
}

.external-result-card__description {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.external-result-card__chip-row {
  gap: 0.45rem;
}

.external-result-card__warning-list {
  gap: 0.45rem;
}

.external-result-card__warning {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2rem;
  padding: 0.45rem 0.75rem;
  border: 1px solid color-mix(in srgb, var(--color-warning) 34%, var(--color-border));
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-warning-soft) 82%, var(--color-surface));
  color: var(--theme-badge-warning-text);
  font-size: 0.88rem;
  font-weight: 600;
}

.external-result-card__warning::before {
  content: 'Hinweis';
  display: inline-flex;
  align-items: center;
  min-height: 1.4rem;
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text-primary);
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.external-result-card__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
  justify-content: space-between;
  padding-top: 0.2rem;
}

.external-result-card__footer-copy {
  gap: 0.35rem;
  max-width: 38rem;
}

.external-result-card__status {
  font-weight: 600;
}

.external-result-card__status--success {
  color: var(--color-success);
}

.external-result-card__status--error {
  color: var(--color-danger);
}

.external-result-card__action-row {
  gap: 0.65rem;
  align-items: center;
  justify-content: flex-end;
}

.external-result-card__link {
  color: var(--color-accent-dark);
  font-weight: 700;
}

@media (max-width: 860px) {
  .external-result-card__layout {
    grid-template-columns: 1fr;
  }
}
</style>
