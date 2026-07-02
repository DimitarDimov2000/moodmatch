<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { importExternalMedia, searchExternal } from '@/api/external';
import { ApiRequestError } from '@/api/client';
import ExternalSearchForm from '@/components/external/ExternalSearchForm.vue';
import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';
import AppMessage from '@/components/common/AppMessage.vue';
import type {
  ExternalImportRequest,
  ExternalSearchResponse,
  ExternalSearchResultResponse,
  MediaType,
} from '@/types/api';

const query = ref('');
const mediaType = ref<MediaType>('FILM');
const loading = ref(false);
const hasSearched = ref(false);
const errorMessage = ref('');
const searchResponse = ref<ExternalSearchResponse | null>(null);
const importStates = ref<Record<string, ImportState>>({});

const fallbackMessage = computed(() => {
  if (!searchResponse.value || searchResponse.value.source !== 'DEMO' || searchResponse.value.warnings.length === 0) {
    return '';
  }

  return searchResponse.value.warnings.join(' ');
});

async function runSearch() {
  const trimmedQuery = query.value.trim();
  hasSearched.value = true;
  errorMessage.value = '';
  importStates.value = {};

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
    });
  } catch (error) {
    searchResponse.value = null;
    errorMessage.value = toUserMessage(error);
  } finally {
    loading.value = false;
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
      description="Filme und Serien nutzen automatisch TMDB, sobald das Backend mit einem API-Key konfiguriert ist. Ohne Key oder fuer andere Typen bleibt der DEMO-Provider als Fallback aktiv."
      tone="info"
    />

    <AppMessage
      v-if="fallbackMessage"
      title="DEMO-Fallback aktiv"
      :description="fallbackMessage"
      tone="info"
    />

    <ExternalSearchForm
      v-model:query="query"
      v-model:media-type="mediaType"
      :submitting="loading"
      @search="runSearch"
    />

    <AppMessage
      v-if="loading"
      title="Externe Suche wird ausgefuehrt"
      description="MoodMatch laedt gerade normalisierte Provider-Treffer fuer die UI."
      tone="info"
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
          <h2>{{ searchResponse.results.length }} Treffer aus {{ searchResponse.source }}</h2>
          <p class="body-muted">
            Importiere einen Treffer, um ihn sofort als eigenes Medium weiterzuverwenden.
          </p>
        </div>
      </header>

      <ExternalSearchResultCard
        v-for="result in searchResponse.results"
        :key="result.externalId"
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

.external-search-view__results-header {
  padding: 1.25rem;
}

.external-search-view__results-header h2,
.external-search-view__results-header p {
  margin: 0.35rem 0 0;
}
</style>
