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
import { i18n } from '@/i18n';
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
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const providerSummaryRows = computed(() => {
  trackLocaleDependency();

  return [
    {
      key: 'film-series',
      label: t('externalSearch.providerSummary.filmSeriesLabel'),
      providers: ['TMDB'],
      note: t('externalSearch.providerSummary.filmSeriesNote'),
    },
    {
      key: 'book',
      label: t('externalSearch.providerSummary.bookLabel'),
      providers: ['Open Library'],
      note: t('externalSearch.providerSummary.bookNote'),
    },
    {
      key: 'anime-manga',
      label: t('externalSearch.providerSummary.animeMangaLabel'),
      providers: ['AniList'],
      note: t('externalSearch.providerSummary.animeMangaNote'),
    },
    {
      key: 'game',
      label: t('externalSearch.providerSummary.gameLabel'),
      providers: ['RAWG', 'Demo'],
      note: t('externalSearch.providerSummary.gameNote'),
    },
    {
      key: 'audio',
      label: t('externalSearch.providerSummary.audioLabel'),
      providers: ['LibriVox', 'Podcast Index'],
      note: t('externalSearch.providerSummary.audioNote'),
    },
    {
      key: 'video',
      label: t('externalSearch.providerSummary.videoLabel'),
      providers: ['YouTube'],
      note: t('externalSearch.providerSummary.videoNote'),
    },
  ];
});

const warningMessage = computed(() => {
  if (!searchResponse.value || searchResponse.value.warnings.length === 0) {
    return '';
  }

  return searchResponse.value.warnings.map((message) => normalizeExternalMessage(message)).join(' ');
});

const resolvedSourceLabel = computed(() => {
  trackLocaleDependency();

  if (!searchResponse.value) {
    return '';
  }

  if (searchResponse.value.source === 'AUTOMATIC') {
    return t('externalSearch.automaticSources');
  }

  return externalSourceLabels[searchResponse.value.source];
});

const providerFilterOptions = computed(() => {
  trackLocaleDependency();

  if (!searchResponse.value) {
    return [];
  }

  return Array.from(new Set(searchResponse.value.results.map((result) => result.source))).map((value) => ({
    value,
    label: externalSourceLabels[value],
  }));
});

const mediaTypeFilterOptions = computed(() => {
  trackLocaleDependency();

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
  trackLocaleDependency();

  if (searchResponse.value?.source !== 'AUTOMATIC') {
    if (searchResponse.value?.source === 'YOUTUBE' && mediaType.value === 'VIDEO') {
      return t('externalSearch.youtubeExplanation', {
        sort: externalSearchSortLabels[sort.value],
      });
    }

    return t('externalSearch.directExplanation');
  }

  return t('externalSearch.autoExplanation');
});
const showPreviewPanel = computed(
  () => youTubeResolveLoading.value || Boolean(youTubeResolveError.value) || Boolean(resolvedYouTubeResult.value),
);

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
    errorMessage.value = t('externalSearch.emptyQuery');
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
        message: response.created
          ? t('externalSearch.importCreated')
          : t('externalSearch.importExisting'),
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
    youTubeResolveError.value = t('externalSearch.emptyResolve');
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

  return t('externalSearch.searchError');
}

function toImportUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return normalizeExternalMessage(error.message);
  }

  return t('externalSearch.importError');
}

function toResolveUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return normalizeExternalMessage(error.message);
  }

  return t('externalSearch.resolveError');
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
    return t('externalSearch.invalidResolve');
  }

  if (message.includes('provider is not configured')) {
    const provider = extractProviderName(message);
    const envHint = message.match(/MOODMATCH_[A-Z0-9_]+/)?.[0];

    if (envHint) {
      return t('externalSearch.providerMissingEnv', { provider, env: envHint });
    }

    return t('externalSearch.providerMissingGeneric', { provider });
  }

  return message;
}

function extractProviderName(message: string): string {
  const providerToken = message.split(' provider')[0]?.trim();

  if (!providerToken) {
    return t('externalSearch.providerFilter');
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
    <header class="page-header external-search-view__header">
      <div>
        <h1 class="page-title">
          {{ t('externalSearch.title') }}
        </h1>
        <p class="page-copy">
          {{ t('externalSearch.intro') }}
        </p>
      </div>

      <div class="page-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--secondary"
        >
          {{ t('externalSearch.manualCreate') }}
        </RouterLink>
      </div>
    </header>

    <div class="external-search-view__entry-grid">
      <section class="external-search-view__lane page-card">
        <div class="external-search-view__lane-copy">
          <p class="eyebrow">
            {{ t('externalSearch.resultsEyebrow') }}
          </p>
          <h2 class="section-title">
            {{ t('externalSearch.resultsTitle') }}
          </h2>
          <p class="body-muted">
            {{ t('externalSearch.resultsCopy') }}
          </p>
        </div>

        <div
          v-if="!hasSearched"
          class="external-search-view__helper-strip"
        >
          <strong>{{ t('externalSearch.readyTitle') }}</strong>
          <span>{{ t('externalSearch.readyDescription') }}</span>
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

      <section class="external-search-view__youtube-stack page-card">
        <div class="external-search-view__lane-copy">
          <p class="eyebrow">
            {{ t('externalSearch.youtubeImportEyebrow') }}
          </p>
          <h2 class="section-title">
            {{ t('externalSearch.youtubeImportTitle') }}
          </h2>
          <p class="body-muted">
            {{ t('externalSearch.youtubeImportCopy') }}
          </p>
        </div>

        <YouTubeUrlImportForm
          v-model:value="youTubeUrl"
          :submitting="youTubeResolveLoading"
          @resolve="resolveYouTube"
        />
      </section>
    </div>

    <section
      v-if="showPreviewPanel"
      class="external-search-view__preview page-card"
    >
      <div class="external-search-view__preview-copy">
        <p class="eyebrow">
          {{ t('externalSearch.previewEyebrow') }}
        </p>
        <h2 class="section-title">
          {{ t('externalSearch.previewTitle') }}
        </h2>
        <p class="body-muted">
          {{ t('externalSearch.previewCopy') }}
        </p>
      </div>

      <AppMessage
        v-if="youTubeResolveLoading"
        :title="t('common.actions.loadingPreview')"
        :description="t('externalSearch.resolveCard')"
        tone="info"
      />

      <AppMessage
        v-else-if="youTubeResolveError"
        :title="t('externalSearch.youtubeResolveWarningTitle')"
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

    <section class="external-search-view__workflow page-card">
      <div class="external-search-view__workflow-copy">
        <p class="eyebrow">
          {{ t('externalSearch.workflowEyebrow') }}
        </p>
        <h2 class="section-title">
          {{ t('externalSearch.workflowTitle') }}
        </h2>
        <p class="body-muted">
          {{ t('externalSearch.workflowCopy') }}
        </p>
      </div>

      <div class="external-search-view__provider-list">
        <article
          v-for="row in providerSummaryRows"
          :key="row.key"
          class="external-search-view__provider-row"
        >
          <p class="external-search-view__provider-label">
            {{ row.label }}
          </p>
          <div class="external-search-view__provider-badges">
            <span
              v-for="provider in row.providers"
              :key="provider"
              class="badge external-search-view__provider-badge"
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
      :title="t('externalSearch.providerHintTitle')"
      :description="warningMessage"
      tone="info"
    />

    <AppMessage
      v-if="loading"
      :title="t('externalSearch.searchingTitle')"
      :description="t('externalSearch.loadingDescription')"
      tone="info"
    />

    <AppMessage
      v-else-if="warningOnlyMessage"
      :title="t('externalSearch.warningOnlyTitle')"
      :description="warningOnlyMessage"
      tone="warning"
    />

    <AppMessage
      v-else-if="errorMessage"
      :title="t('externalSearch.searchFailedTitle')"
      :description="errorMessage"
      tone="error"
    >
      <div class="external-search-view__actions">
        <button
          class="button button--secondary"
          type="button"
          @click="runSearch"
        >
          {{ t('common.actions.retry') }}
        </button>
      </div>
    </AppMessage>

    <AppMessage
      v-else-if="searchResponse && searchResponse.results.length === 0"
      :title="t('externalSearch.emptyTitle')"
      :description="t('externalSearch.emptyDescription')"
    />

    <section
      v-else-if="searchResponse"
      class="external-search-view__results"
    >
      <header class="external-search-view__results-header page-card">
        <div>
          <p class="eyebrow">
            {{ t('externalSearch.resultsEyebrow') }}
          </p>
          <h2>{{ t('externalSearch.resultsCount', { count: filteredResults.length, source: resolvedSourceLabel }) }}</h2>
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
            <span>{{ t('externalSearch.providerFilter') }}</span>
            <select
              v-model="providerFilter"
              class="input"
              name="resultProviderFilter"
            >
              <option value="ALL">
                {{ t('externalSearch.all') }}
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
            <span>{{ t('externalSearch.mediaTypeFilter') }}</span>
            <select
              v-model="resultMediaTypeFilter"
              class="input"
              name="resultMediaTypeFilter"
            >
              <option value="ALL">
                {{ t('externalSearch.all') }}
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
        :title="t('externalSearch.filteredEmptyTitle')"
        :description="t('externalSearch.filteredEmptyDescription')"
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

.external-search-view__header .page-title {
  max-width: none;
}

.external-search-view__header .page-copy {
  max-width: 42rem;
}

.external-search-view__workflow {
  display: grid;
  gap: 0.72rem;
  padding: clamp(0.92rem, 2vw, 1.08rem);
}

.external-search-view__workflow-copy,
.external-search-view__lane-copy,
.external-search-view__preview-copy {
  display: grid;
  gap: 0.32rem;
}

.external-search-view__workflow-copy p,
.external-search-view__provider-label,
.external-search-view__provider-note,
.external-search-view__lane-copy p,
.external-search-view__preview-copy p {
  margin: 0;
}

.external-search-view__provider-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.external-search-view__provider-list {
  display: grid;
  gap: 0.48rem;
}

.external-search-view__provider-row {
  display: grid;
  gap: 0.4rem 0.8rem;
  grid-template-columns: minmax(148px, 0.4fr) minmax(176px, 0.58fr) minmax(0, 1fr);
  align-items: start;
  min-height: 0;
  padding: 0.6rem 0.74rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 38%),
    color-mix(in srgb, var(--color-surface-secondary) 82%, transparent);
}

.external-search-view__provider-label {
  color: var(--color-text-primary);
  font-size: 0.9rem;
  font-weight: 700;
}

.external-search-view__provider-badge {
  background: color-mix(in srgb, var(--color-surface) 84%, var(--color-surface-secondary));
  border-color: color-mix(in srgb, var(--color-border-strong) 68%, transparent);
  color: var(--color-text-primary);
}

.external-search-view__provider-note {
  font-size: 0.82rem;
  line-height: 1.36;
}

.external-search-view__entry-grid {
  display: grid;
  gap: 0.95rem;
  grid-template-columns: minmax(0, 1.04fr) minmax(320px, 0.96fr);
  align-items: stretch;
}

.external-search-view__lane,
.external-search-view__results,
.external-search-view__youtube-stack,
.external-search-view__preview {
  display: grid;
  gap: 0.88rem;
}

.external-search-view__lane,
.external-search-view__youtube-stack {
  align-content: start;
  padding: clamp(0.92rem, 1.8vw, 1.08rem);
}

.external-search-view__lane-copy {
  min-height: 5.2rem;
}

.external-search-view__lane-copy {
  align-content: start;
}

.external-search-view__helper-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem 0.7rem;
  align-items: center;
  padding: 0.44rem 0.6rem;
  border: 1px solid color-mix(in srgb, var(--color-info) 20%, var(--color-border));
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 42%),
    color-mix(in srgb, var(--color-info-soft) 44%, var(--color-surface));
  color: var(--color-text-secondary);
  font-size: 0.84rem;
  line-height: 1.34;
}

.external-search-view__helper-strip strong,
.external-search-view__helper-strip span {
  margin: 0;
}

.external-search-view__helper-strip strong {
  color: var(--color-text-primary);
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.external-search-view__preview {
  align-content: start;
  padding: clamp(0.9rem, 1.9vw, 1.05rem);
}

.external-search-view__results-header {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.9fr);
  align-items: end;
  padding: 1rem 1.05rem;
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

  .external-search-view__results-header {
    grid-template-columns: 1fr;
  }

  .external-search-view__provider-row {
    grid-template-columns: 1fr;
  }
}
</style>
