<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { importExternalMedia, resolveExternalUrl, searchExternal } from '@/api/external';
import { ApiRequestError } from '@/api/client';
import {
  defaultSourceSelectionForMediaType,
  externalSourceLabels,
  isSourceSelectionValid,
  type ExternalSourceSelection,
} from '@/components/external/external-options';
import { mediaTypeLabels } from '@/components/media/media-options';
import ExternalSearchForm from '@/components/external/ExternalSearchForm.vue';
import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';
import YouTubeUrlImportForm from '@/components/external/YouTubeUrlImportForm.vue';
import AppMessage from '@/components/common/AppMessage.vue';
import type {
  ExternalImportRequest,
  ExternalSearchResponse,
  ExternalSearchResultResponse,
  MediaType,
} from '@/types/api';

const query = ref('');
const mediaType = ref<MediaType>('FILM');
const source = ref<ExternalSourceSelection>('AUTO');
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

const warningMessage = computed(() => {
  if (!searchResponse.value || searchResponse.value.warnings.length === 0) {
    return '';
  }

  return searchResponse.value.warnings.join(' ');
});

const resolvedSourceLabel = computed(() => {
  if (!searchResponse.value) {
    return '';
  }

  if (searchResponse.value.source === 'AUTOMATIC') {
    return 'passenden Quellen';
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
    return 'Importiere einen Treffer, um ihn sofort als eigenes Medium weiterzuverwenden.';
  }

  return 'Automatic searches all suitable providers for the selected media type.';
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
    });
  } catch (error) {
    searchResponse.value = null;
    if (error instanceof ApiRequestError && isProviderConfigurationMessage(error.message)) {
      warningOnlyMessage.value = error.message;
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
    return error.message;
  }

  return 'Die externe Suche konnte gerade nicht geladen werden.';
}

function toImportUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return 'Der Import konnte gerade nicht abgeschlossen werden.';
}

function toResolveUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
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

interface ImportState {
  importing: boolean;
  error: string;
  message: string;
  mediaId: string | null;
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          External Search
        </p>
        <h1 class="page-title">
          Externe Medien suchen und importieren
        </h1>
        <p class="page-copy">
          Suche aus MoodMatch heraus nach externen Titeln, pruefe die normalisierten Metadaten
          und speichere passende Treffer direkt in deine eigene Mediathek.
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

    <AppMessage
      title="Provider-Verhalten"
      description="TMDB, Open Library, LibriVox, RAWG und AniList sind aktiv. Podcast Index ist fuer Podcast-Shows verfuegbar, sobald Backend-Key und Backend-Secret gesetzt sind. AniList bleibt eine Quelle fuer Anime/Manga; importierte Anime-Filme, Anime-Serien und Manga landen als Film, Serie oder Buch in deiner Mediathek. YouTube unterstuetzt jetzt offizielle Videosuche per Suchbegriff und weiterhin den separaten URL-Import fuer bekannte Links oder IDs."
      tone="info"
    />

    <AppMessage
      v-if="warningMessage"
      title="Provider-Hinweis"
      :description="warningMessage"
      tone="info"
    />

    <ExternalSearchForm
      v-model:query="query"
      v-model:source="source"
      :media-type="mediaType"
      :submitting="loading"
      @update:media-type="updateMediaType"
      @search="runSearch"
    />

    <section class="external-search-view__youtube-stack">
      <header class="page-card external-search-view__youtube-header">
        <div>
          <p class="eyebrow">
            YouTube URL Import
          </p>
          <h2>YouTube-Video per URL oder ID importieren</h2>
          <p class="body-muted">
            Fuege eine YouTube-URL oder Video-ID ein, pruefe die Vorschau und importiere das Video
            als normales VIDEO-Medium in deine Mediathek.
          </p>
        </div>
      </header>

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
        tone="error"
      />

      <ExternalSearchResultCard
        v-else-if="resolvedYouTubeResult"
        :result="resolvedYouTubeResult"
        :is-importing="getImportState(resolvedYouTubeResult).importing"
        :import-error="getImportState(resolvedYouTubeResult).error"
        :import-message="getImportState(resolvedYouTubeResult).message"
        :imported-media-id="getImportState(resolvedYouTubeResult).mediaId"
        @import="importResult"
      />
    </section>

    <AppMessage
      v-if="loading"
      title="Externe Suche wird ausgefuehrt"
      description="MoodMatch laedt gerade normalisierte Provider-Treffer fuer die UI."
      tone="info"
    />

    <AppMessage
      v-else-if="warningOnlyMessage"
      title="Provider-Hinweis"
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
      title="Noch keine Suche gestartet"
      description="Waehle einen Medientyp, gib einen Suchbegriff ein und importiere interessante Treffer direkt in deine Mediathek."
    />

    <AppMessage
      v-else-if="searchResponse && searchResponse.results.length === 0"
      title="Keine Ergebnisse gefunden"
      description="Fuer diese Suche wurden keine passenden externen Titel gefunden."
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
        @import="importResult"
      />
    </section>
  </section>
</template>

<style scoped>
.external-search-view__actions {
  margin-top: 1rem;
}

.external-search-view__results {
  display: grid;
  gap: 1rem;
}

.external-search-view__youtube-stack {
  display: grid;
  gap: 1rem;
}

.external-search-view__youtube-header {
  padding: 1.25rem;
}

.external-search-view__youtube-header h2,
.external-search-view__youtube-header p {
  margin: 0.35rem 0 0;
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
</style>
