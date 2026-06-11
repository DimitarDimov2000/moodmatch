<script setup lang="ts">
import { ref } from 'vue';

import { searchExternal } from '@/api/external';
import { ApiRequestError } from '@/api/client';
import ExternalSearchForm from '@/components/external/ExternalSearchForm.vue';
import ExternalSearchResultCard from '@/components/external/ExternalSearchResultCard.vue';
import AppMessage from '@/components/common/AppMessage.vue';
import type { ExternalSearchResponse, MediaType } from '@/types/api';

const query = ref('');
const mediaType = ref<MediaType>('FILM');
const loading = ref(false);
const hasSearched = ref(false);
const errorMessage = ref('');
const searchResponse = ref<ExternalSearchResponse | null>(null);

async function runSearch() {
  const trimmedQuery = query.value.trim();
  hasSearched.value = true;
  errorMessage.value = '';

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

function toUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return 'Die externe Demo-Suche konnte gerade nicht geladen werden.';
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
          Provider-Preview ohne Live-API
        </h1>
        <p class="page-copy">
          Diese Phase prueft die adapterbasierte Suche mit einem deterministischen Offline-Demo-Provider.
          Ergebnisse sind nur Vorschau, werden noch nicht importiert und aendern keine bestehenden MoodMatch-Regeln.
        </p>
      </div>
    </header>

    <AppMessage
      title="Phase 18 Vorschau"
      description="Die Suche nutzt ausschliesslich normalisierte Demo-Daten. Es werden weder API-Keys noch externe HTTP-Aufrufe oder Import-Schritte verwendet."
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
      title="Demo-Suche wird ausgefuehrt"
      description="Der Offline-Provider filtert gerade den festen Katalog und normalisiert die Treffer fuer die UI."
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
      description="Waehle einen Medientyp, gib einen Suchbegriff ein und pruefe die normalisierte Resultatform des Demo-Providers."
    />

    <AppMessage
      v-else-if="searchResponse && searchResponse.results.length === 0"
      title="Keine Demo-Ergebnisse gefunden"
      description="Der feste Offline-Katalog enthaelt fuer diese Suche keine passenden Eintraege."
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
            Preview-only. Import in die Medienbibliothek folgt in einer spaeteren Phase.
          </p>
        </div>
      </header>

      <ExternalSearchResultCard
        v-for="result in searchResponse.results"
        :key="result.externalId"
        :result="result"
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
