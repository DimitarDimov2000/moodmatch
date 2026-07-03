<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { importExternalMedia, resolveExternalUrl, searchExternal } from '@/api/external';
import { ApiRequestError } from '@/api/client';
import AppMessage from '@/components/common/AppMessage.vue';
import {
  defaultSourceSelectionForMediaType,
  externalSourceLabels,
  externalSearchSortLabels,
  isSourceSelectionValid,
  type ExternalSourceSelection,
} from '@/components/external/external-options';
import ExternalSearchForm from '@/components/external/ExternalSearchForm.vue';
import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';
import YouTubeUrlImportForm from '@/components/external/YouTubeUrlImportForm.vue';
import { mediaTypeLabels } from '@/components/media/media-options';
import { getExternalSourceLabel } from '@/components/media/media-presentation';
import type {
  ExternalImportRequest,
  ExternalSearchResponse,
  ExternalSearchResultResponse,
  ExternalSearchSort,
  MediaType,
} from '@/types/api';

const query = ref('');
const mediaType = ref<MediaType>('FILM');
const source = ref<ExternalSourceSelection>('AUTO');
const sort = ref<ExternalSearchSort>('relevance');
const loading = ref(false);
const hasSearched = ref(false);
const errorMessage = ref('');
const warningOnlyMessage = ref('');
const searchResponse = ref<ExternalSearchResponse | null>(null);
const importStates = ref<Record<string, ImportState>>({});
const youTubeUrl = ref('');
const youTubeResolveLoading = ref(false);
const youTubeResolveError = ref('');
const resolvedYouTubeResult = ref<ExternalSearchResultResponse | null>(null);
const providerFilter = ref<'ALL' | ExternalSearchResultResponse['source']>('ALL');
const resultMediaTypeFilter = ref<'ALL' | MediaType>('ALL');

const providerSummaryRows = [
  {
    label: 'Film & Serie',
    providers: ['TMDB', 'AniList'],
    note: 'Anime-Filme bleiben Film, Anime-Serien bleiben Serie.',
  },
  {
    label: 'Buch',
    providers: ['Open Library', 'AniList'],
    note: 'Manga und Light Novels landen als normale Buecher in deiner Mediathek.',
  },
  {
    label: 'Spiel',
    providers: ['RAWG', 'Demo'],
    note: 'Wenn RAWG fehlt, bleibt der Demo-Fallback verfuegbar.',
  },
  {
    label: 'Hoerbuch & Podcast',
    providers: ['LibriVox', 'Podcast Index'],
    note: 'Podcast Index braucht Backend-Key und Secret, LibriVox deckt gemeinfreie Hoerbuecher ab.',
  },
  {
    label: 'Video',
    providers: ['YouTube'],
    note: 'YouTube-Suche und der separate URL-Import bleiben bewusst getrennt.',
  },
] as const;

const warningMessage = computed(() => {
  if (!searchResponse.value || searchResponse.value.warnings.length === 0) {
    return '';
  }

  return searchResponse.value.warnings.map((message) => normalizeExternalMessage(message)).join(' ');
});

const resolvedSourceLabel = computed(() => {
  if (!searchResponse.value) {
    return '';
  }

  if (searchResponse.value.source === 'AUTOMATIC') {
    return 'automatischen Quellen';
  }

  return externalSourceLabels[searchResponse.value.source];
});

const providerFilterOptions = computed(() => {
  if (!searchResponse.value) {
    return [];
  }

  return Array.from(new Set(searchResponse.value.results.map((result) => result.source))).map((value) => ({
    value,
    label: externalSourceLabels[value],
  }));
});

const mediaTypeFilterOptions = computed(() => {
  if (!searchResponse.value) {
    return [];
  }

  return Array.from(new Set(searchResponse.value.results.map((result) => result.mediaType))).map((value) => ({
    value,
    label: mediaTypeLabels[value],
  }));
});

const filteredResults = computed(() => {
  if (!searchResponse.value) {
    return [];
  }

  return searchResponse.value.results.filter((result) => {
    if (providerFilter.value !== 'ALL' && result.source !== providerFilter.value) {
      return false;
    }

    if (resultMediaTypeFilter.value !== 'ALL' && result.mediaType !== resultMediaTypeFilter.value) {
      return false;
    }

    return true;
  });
});

const automaticSearchExplanation = computed(() => {
  if (searchResponse.value?.source !== 'AUTOMATIC') {
    if (searchResponse.value?.source === 'YOUTUBE' && mediaType.value === 'VIDEO') {
      return `Pruefe die Treffer, importiere passende Videos direkt in die Mediathek und nutze dabei die Sortierung ${externalSearchSortLabels[sort.value]}.`;
    }

    return 'Pruefe die normalisierten Metadaten und uebernimm passende Treffer direkt in deine Mediathek.';
  }

  return 'Automatisch kombiniert fuer diesen Medientyp alle passenden Quellen und zeigt dir nur normalisierte Treffer fuer den Import.';
});

async function runSearch() {
  const trimmedQuery = query.value.trim();
  hasSearched.value = true;
  errorMessage.value = '';
  warningOnlyMessage.value = '';
  importStates.value = {};
  providerFilter.value = 'ALL';
  resultMediaTypeFilter.value = 'ALL';

  if (!trimmedQuery) {
    searchResponse.value = null;
    errorMessage.value = 'Bitte gib zuerst einen Suchbegriff ein.';
    return;
  }

  loading.value = true;

  try {
    searchResponse.value = await searchExternal({
      query: trimmedQuery,
      mediaType: mediaType.value,
      source: source.value === 'AUTO' ? undefined : source.value,
      sort: mediaType.value === 'VIDEO' && source.value === 'YOUTUBE' ? sort.value : undefined,
    });
  } catch (error) {
    searchResponse.value = null;

    if (error instanceof ApiRequestError && isProviderConfigurationMessage(error.message)) {
      warningOnlyMessage.value = normalizeExternalMessage(error.message);
      return;
    }

    errorMessage.value = toUserMessage(error);
  } finally {
    loading.value = false;
  }
}

function updateMediaType(value: MediaType) {
  mediaType.value = value;

  if (!isSourceSelectionValid(value, source.value)) {
    source.value = defaultSourceSelectionForMediaType(value);
  }

  if (value !== 'VIDEO') {
    sort.value = 'relevance';
  }
}

async function importResult(result: ExternalSearchResultResponse) {
  const key = resultKey(result);
  importStates.value = {
    ...importStates.value,
    [key]: {
      importing: true,
      error: '',
      message: '',
      mediaId: null,
      created: null,
    },
  };

  try {
    const response = await importExternalMedia(toImportRequest(result));
    importStates.value = {
      ...importStates.value,
      [key]: {
        importing: false,
        error: '',
        message: response.message,
        mediaId: response.media.id,
        created: response.created,
      },
    };
  } catch (error) {
    importStates.value = {
      ...importStates.value,
      [key]: {
        importing: false,
        error: toImportUserMessage(error),
        message: '',
        mediaId: null,
        created: null,
      },
    };
  }
}

async function resolveYouTube() {
  const trimmedValue = youTubeUrl.value.trim();
  youTubeResolveError.value = '';
  resolvedYouTubeResult.value = null;

  if (!trimmedValue) {
    youTubeResolveError.value = 'Bitte gib zuerst eine YouTube-URL oder Video-ID ein.';
    return;
  }

  youTubeResolveLoading.value = true;

  try {
    resolvedYouTubeResult.value = await resolveExternalUrl({
      source: 'YOUTUBE',
      url: trimmedValue,
    });
  } catch (error) {
    youTubeResolveError.value = toResolveUserMessage(error);
  } finally {
    youTubeResolveLoading.value = false;
  }
}

function toUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return normalizeExternalMessage(error.message);
  }

  return 'Die externe Suche konnte gerade nicht geladen werden.';
}

function toImportUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return normalizeExternalMessage(error.message);
  }

  return 'Der Import konnte gerade nicht abgeschlossen werden.';
}

function toResolveUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return normalizeExternalMessage(error.message);
  }

  return 'Die YouTube-URL konnte gerade nicht aufgeloest werden.';
}

function isProviderConfigurationMessage(message: string): boolean {
  return message.includes('provider is not configured');
}

function resultKey(result: ExternalSearchResultResponse) {
  return `${result.source}:${result.externalId}`;
}

function getImportState(result: ExternalSearchResultResponse): ImportState {
  return (
    importStates.value[resultKey(result)] ?? {
      importing: false,
      error: '',
      message: '',
      mediaId: null,
      created: null,
    }
  );
}

function toImportRequest(result: ExternalSearchResultResponse): ExternalImportRequest {
  return {
    source: result.source,
    externalId: result.externalId,
    mediaType: result.mediaType,
    title: result.title,
    originalTitle: result.originalTitle,
    creatorNames: result.creatorNames,
    description: result.description,
    releaseYear: result.releaseYear,
    coverUrl: result.coverUrl,
    sourceUrl: result.sourceUrl,
    externalGenres: result.externalGenres,
    externalSubjects: result.externalSubjects,
    attribution: result.attribution,
  };
}

function normalizeExternalMessage(message: string): string {
  if (message === 'Enter a valid YouTube URL or video ID.') {
    return 'Bitte gib eine gueltige YouTube-URL oder Video-ID ein.';
  }

  if (message.includes('provider is not configured')) {
    const provider = extractProviderName(message);
    const envHint = message.match(/MOODMATCH_[A-Z0-9_]+/)?.[0];

    if (envHint) {
      return `${provider} ist aktuell nicht verbunden. Hinterlege ${envHint} im Backend, um Suche oder Import zu nutzen.`;
    }

    return `${provider} ist aktuell nicht verbunden. Pruefe die Backend-Konfiguration und versuche es danach erneut.`;
  }

  return message;
}

function extractProviderName(message: string): string {
  const providerToken = message.split(' provider')[0]?.trim();

  if (!providerToken) {
    return 'Der Provider';
  }

  const sourceKey = providerToken.toUpperCase().replace(/\s+/g, '_');

  return getExternalSourceLabel(sourceKey as Parameters<typeof getExternalSourceLabel>[0]) || providerToken;
}

interface ImportState {
  importing: boolean;
  error: string;
  message: string;
  mediaId: string | null;
  created: boolean | null;
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Externe Suche
        </p>
        <h1 class="page-title">
          Externe Medien suchen und importieren
        </h1>
        <p class="page-copy">
          Suche in passenden Quellen, pruefe die normalisierten Metadaten und uebernimm
          interessante Treffer direkt in deine Mediathek.
        </p>
      </div>

      <div class="page-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--secondary"
        >
          Medium manuell anlegen
        </RouterLink>
      </div>
    </header>

    <section class="external-search-view__workflow page-card">
      <div class="external-search-view__workflow-copy">
        <p class="eyebrow">
          Import-Workflow
        </p>
        <h2 class="section-title">
          Suchen, pruefen, importieren
        </h2>
        <p class="body-muted">
          Erst suchst du in externen Quellen, dann pruefst du die normalisierten Felder
          und importierst nur die Treffer, die wirklich in deine Sammlung passen.
        </p>
      </div>

      <div class="external-search-view__step-row">
        <span class="badge">1 Suche</span>
        <span class="badge">2 Vorschau</span>
        <span class="badge badge--accent">3 Import in die Mediathek</span>
      </div>

      <div class="external-search-view__provider-grid">
        <article
          v-for="row in providerSummaryRows"
          :key="row.label"
          class="external-search-view__provider-card"
        >
          <p class="external-search-view__provider-label">
            {{ row.label }}
          </p>
          <div class="external-search-view__provider-badges">
            <span
              v-for="provider in row.providers"
              :key="provider"
              class="badge badge--accent"
            >
              {{ provider }}
            </span>
          </div>
          <p class="body-muted external-search-view__provider-note">
            {{ row.note }}
          </p>
        </article>
      </div>
    </section>

    <AppMessage
      v-if="warningMessage"
      title="Provider-Hinweis"
      :description="warningMessage"
      tone="info"
    />

    <div class="external-search-view__entry-grid">
      <section class="external-search-view__lane">
        <div class="external-search-view__lane-copy">
          <p class="eyebrow">
            Externe Treffer
          </p>
          <h2 class="section-title">
            Suche ueber Provider
          </h2>
          <p class="body-muted">
            Waehle Medientyp und Quelle, starte die Suche und vergleiche die normalisierten Treffer
            vor dem Import.
          </p>
        </div>

        <ExternalSearchForm
          v-model:query="query"
          v-model:source="source"
          v-model:sort="sort"
          :media-type="mediaType"
          :submitting="loading"
          @update:media-type="updateMediaType"
          @search="runSearch"
        />
      </section>

      <section class="external-search-view__youtube-stack">
        <div class="external-search-view__lane-copy">
          <p class="eyebrow">
            YouTube Direktimport
          </p>
          <h2 class="section-title">
            YouTube-Video per URL oder ID pruefen
          </h2>
          <p class="body-muted">
            Fuege eine bekannte URL oder Video-ID ein, lade die Vorschau und importiere das Video
            anschliessend als normales Medium.
          </p>
        </div>

        <YouTubeUrlImportForm
          v-model:value="youTubeUrl"
          :submitting="youTubeResolveLoading"
          @resolve="resolveYouTube"
        />

        <AppMessage
          v-if="youTubeResolveLoading"
          title="YouTube-Vorschau wird geladen"
          description="MoodMatch laedt die offiziellen YouTube-Metadaten fuer diese URL oder Video-ID."
          tone="info"
        />

        <AppMessage
          v-else-if="youTubeResolveError"
          title="YouTube-Import konnte nicht vorbereitet werden"
          :description="youTubeResolveError"
          tone="warning"
        />

        <ExternalSearchResultCard
          v-else-if="resolvedYouTubeResult"
          :result="resolvedYouTubeResult"
          :is-importing="getImportState(resolvedYouTubeResult).importing"
          :import-error="getImportState(resolvedYouTubeResult).error"
          :import-message="getImportState(resolvedYouTubeResult).message"
          :imported-media-id="getImportState(resolvedYouTubeResult).mediaId"
          :import-created="getImportState(resolvedYouTubeResult).created"
          @import="importResult"
        />
      </section>
    </div>

    <AppMessage
      v-if="loading"
      title="Suche laeuft"
      description="MoodMatch sammelt gerade normalisierte Treffer aus den passenden Quellen."
      tone="info"
    />

    <AppMessage
      v-else-if="warningOnlyMessage"
      title="Provider aktuell nicht bereit"
      :description="warningOnlyMessage"
      tone="warning"
    />

    <AppMessage
      v-else-if="errorMessage"
      title="Suche konnte nicht abgeschlossen werden"
      :description="errorMessage"
      tone="error"
    >
      <div class="external-search-view__actions">
        <button
          class="button button--secondary"
          type="button"
          @click="runSearch"
        >
          Erneut versuchen
        </button>
      </div>
    </AppMessage>

    <AppMessage
      v-else-if="!hasSearched"
      title="Bereit fuer die erste Suche"
      description="Gib einen Suchbegriff ein, pruefe danach die Vorschau und importiere nur die Treffer, die du wirklich behalten willst."
    />

    <AppMessage
      v-else-if="searchResponse && searchResponse.results.length === 0"
      title="Keine Ergebnisse gefunden"
      description="Fuer diese Kombination aus Suchbegriff, Medientyp und Quelle wurden keine passenden Treffer gefunden. Probiere einen anderen Begriff oder wechsle die Quelle."
    />

    <section
      v-else-if="searchResponse"
      class="external-search-view__results"
    >
      <header class="external-search-view__results-header page-card">
        <div>
          <p class="eyebrow">
            Resultate
          </p>
          <h2>{{ filteredResults.length }} Treffer aus {{ resolvedSourceLabel }}</h2>
          <p class="body-muted">
            {{ automaticSearchExplanation }}
          </p>
        </div>

        <div
          v-if="providerFilterOptions.length > 1 || mediaTypeFilterOptions.length > 1"
          class="external-search-view__filter-grid"
        >
          <label
            v-if="providerFilterOptions.length > 1"
            class="external-search-view__filter"
          >
            <span>Provider</span>
            <select
              v-model="providerFilter"
              class="input"
              name="resultProviderFilter"
            >
              <option value="ALL">
                Alle
              </option>
              <option
                v-for="option in providerFilterOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
          </label>

          <label
            v-if="mediaTypeFilterOptions.length > 1"
            class="external-search-view__filter"
          >
            <span>Medientyp</span>
            <select
              v-model="resultMediaTypeFilter"
              class="input"
              name="resultMediaTypeFilter"
            >
              <option value="ALL">
                Alle
              </option>
              <option
                v-for="option in mediaTypeFilterOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
          </label>
        </div>
      </header>

      <AppMessage
        v-if="filteredResults.length === 0"
        title="Keine Treffer fuer die aktuellen Filter"
        description="Passe den Provider- oder Medientyp-Filter an, um weitere Ergebnisse anzuzeigen."
        tone="info"
      />

      <ExternalSearchResultCard
        v-for="result in filteredResults"
        :key="resultKey(result)"
        :result="result"
        :is-importing="getImportState(result).importing"
        :import-error="getImportState(result).error"
        :import-message="getImportState(result).message"
        :imported-media-id="getImportState(result).mediaId"
        :import-created="getImportState(result).created"
        @import="importResult"
      />
    </section>
  </section>
</template>

<style scoped>
.external-search-view__actions {
  margin-top: 1rem;
}

.external-search-view__workflow {
  display: grid;
  gap: 1rem;
  padding: clamp(1rem, 2.4vw, 1.2rem);
}

.external-search-view__workflow-copy,
.external-search-view__lane-copy {
  display: grid;
  gap: 0.4rem;
}

.external-search-view__workflow-copy p,
.external-search-view__provider-label,
.external-search-view__provider-note,
.external-search-view__lane-copy p {
  margin: 0;
}

.external-search-view__step-row,
.external-search-view__provider-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.external-search-view__provider-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.external-search-view__provider-card {
  display: grid;
  gap: 0.45rem;
  padding: 0.9rem 0.95rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-surface-secondary) 78%, transparent);
}

.external-search-view__provider-label {
  color: var(--color-text-primary);
  font-size: 0.92rem;
  font-weight: 700;
}

.external-search-view__provider-note {
  font-size: 0.88rem;
}

.external-search-view__entry-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  align-items: start;
}

.external-search-view__lane,
.external-search-view__results,
.external-search-view__youtube-stack {
  display: grid;
  gap: 1rem;
}

.external-search-view__results-header {
  padding: 1.25rem;
}

.external-search-view__results-header h2,
.external-search-view__results-header p {
  margin: 0.35rem 0 0;
}

.external-search-view__filter-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  align-items: end;
}

.external-search-view__filter {
  display: grid;
  gap: 0.35rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-secondary);
}

@media (max-width: 980px) {
  .external-search-view__entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
