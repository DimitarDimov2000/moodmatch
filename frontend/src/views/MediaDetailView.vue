<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import {
  getMediaById,
  replaceMediaTags,
  updateMedia,
  updateMediaFavourite,
  updateMediaStatus,
} from '@/api/media';
import { listTags } from '@/api/tags';
import AppMessage from '@/components/common/AppMessage.vue';
import MediaForm, { type MediaFormSubmitPayload } from '@/components/media/MediaForm.vue';
import MediaStatusControls from '@/components/media/MediaStatusControls.vue';
import {
  consumptionStatusLabels,
  mediaTypeLabels,
  metadataOriginLabels,
  sourceTypeLabels,
} from '@/components/media/media-options';
import TagCategoryList from '@/components/tags/TagCategoryList.vue';
import type {
  MediaResponse,
  TagResponse,
  UpdateMediaConsumptionStatusRequest,
  UpdateMediaFavouriteRequest,
} from '@/types/api';

const route = useRoute();
const media = ref<MediaResponse | null>(null);
const availableTags = ref<TagResponse[]>([]);
const loading = ref(true);
const saving = ref(false);
const errorMessage = ref('');
const apiErrors = ref<Record<string, string>>({});

const mediaId = computed(() => String(route.params.id));

onMounted(async () => {
  await loadPage();
});

async function loadPage() {
  loading.value = true;
  errorMessage.value = '';

  try {
    const [mediaResponse, tagResponse] = await Promise.all([
      getMediaById(mediaId.value),
      listTags(),
    ]);

    media.value = mediaResponse;
    availableTags.value = tagResponse;
  } catch (error) {
    errorMessage.value = toUserMessage(error);
  } finally {
    loading.value = false;
  }
}

async function handleSubmit(payload: MediaFormSubmitPayload) {
  if (!media.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = '';
  apiErrors.value = {};

  try {
    const updatedMedia = await updateMedia(media.value.id, payload.media);
    const nextMedia =
      updatedMedia.tags.map((tag) => tag.id).sort().join(',') === payload.tagIds.slice().sort().join(',')
        ? updatedMedia
        : await replaceMediaTags(updatedMedia.id, { tagIds: payload.tagIds });

    media.value = nextMedia;
  } catch (error) {
    if (error instanceof ApiRequestError) {
      errorMessage.value = error.message;
      apiErrors.value = Object.fromEntries(
        error.details
          .filter((detail) => detail.field)
          .map((detail) => [detail.field as string, detail.message]),
      );
    } else {
      errorMessage.value = 'Das Medium konnte nicht aktualisiert werden.';
    }
  } finally {
    saving.value = false;
  }
}

async function handleStatusUpdate(request: UpdateMediaConsumptionStatusRequest) {
  if (!media.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = '';

  try {
    media.value = await updateMediaStatus(media.value.id, request);
  } catch (error) {
    errorMessage.value = toUserMessage(error);
  } finally {
    saving.value = false;
  }
}

async function handleFavouriteUpdate(request: UpdateMediaFavouriteRequest) {
  if (!media.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = '';

  try {
    media.value = await updateMediaFavourite(media.value.id, request);
  } catch (error) {
    errorMessage.value = toUserMessage(error);
  } finally {
    saving.value = false;
  }
}

function toUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return 'Die Daten konnten nicht verarbeitet werden.';
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Medien Detail
        </p>
        <h1 class="page-title">
          {{ media?.title ?? 'Medium laden' }}
        </h1>
        <p class="page-copy">
          Vollansicht fuer Bearbeitung, Tag-Pflege und API-gestuetzte Statusaenderungen.
        </p>
      </div>

      <div class="page-actions">
        <RouterLink
          :to="{ name: 'media-list' }"
          class="button button--secondary"
        >
          Zur Liste
        </RouterLink>
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--ghost"
        >
          Neues Medium
        </RouterLink>
      </div>
    </header>

    <AppMessage
      v-if="loading"
      title="Medium wird geladen"
      description="Details, Tags und Bearbeitungszustand werden vorbereitet."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage && !media"
      title="Detailansicht nicht verfuegbar"
      :description="errorMessage"
      tone="error"
    >
      <div class="media-detail__actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadPage"
        >
          Erneut versuchen
        </button>
      </div>
    </AppMessage>

    <template v-else-if="media">
      <AppMessage
        v-if="errorMessage"
        title="Letzte Aktion fehlgeschlagen"
        :description="errorMessage"
        tone="error"
      />

      <section class="media-detail__hero">
        <article class="page-card media-detail__overview">
          <div class="media-detail__overview-copy">
            <div class="media-detail__pill-row">
              <span class="media-detail__pill">{{ mediaTypeLabels[media.mediaType] }}</span>
              <span class="media-detail__pill">{{ consumptionStatusLabels[media.consumptionStatus] }}</span>
              <span
                v-if="media.isFavourite"
                class="media-detail__pill media-detail__pill--favourite"
              >
                Favorit
              </span>
            </div>

            <p
              v-if="media.description"
              class="body-muted"
            >
              {{ media.description }}
            </p>

            <dl class="media-detail__facts">
              <div>
                <dt>Bewertung</dt>
                <dd>{{ media.rating ?? 'Keine' }}</dd>
              </div>
              <div>
                <dt>Quelle</dt>
                <dd>{{ sourceTypeLabels[media.sourceType] }}</dd>
              </div>
              <div>
                <dt>Metadaten</dt>
                <dd>{{ media.metadataOrigin ? metadataOriginLabels[media.metadataOrigin] : 'Keine Angabe' }}</dd>
              </div>
            </dl>
          </div>

          <img
            v-if="media.coverUrl"
            :src="media.coverUrl"
            :alt="`Cover von ${media.title}`"
            class="media-detail__cover"
          >
          <div
            v-else
            class="media-detail__cover media-detail__cover--placeholder"
          >
            {{ media.title.slice(0, 1).toUpperCase() }}
          </div>
        </article>

        <MediaStatusControls
          :media="media"
          :pending="saving"
          @update-status="handleStatusUpdate"
          @update-favourite="handleFavouriteUpdate"
        />
      </section>

      <section class="media-detail__layout">
        <div class="media-detail__main">
          <MediaForm
            mode="edit"
            :initial-media="media"
            :available-tags="availableTags"
            :submitting="saving"
            :api-errors="apiErrors"
            submit-label="Aenderungen speichern"
            @submit="handleSubmit"
          />
        </div>

        <div class="media-detail__side">
          <TagCategoryList
            :tags="media.tags"
            title="Aktive Tags"
            compact
          />

          <AppMessage
            title="Externe Referenzen"
            :description="media.externalReferences.length ? `${media.externalReferences.length} Quelle(n) verknuepft.` : 'Noch keine externen Referenzen vorhanden.'"
            tone="neutral"
          />
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.media-detail__hero {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr);
}

.media-detail__overview {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 1fr) 12rem;
  padding: 1.5rem;
}

.media-detail__pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.media-detail__pill {
  display: inline-flex;
  padding: 0.4rem 0.7rem;
  border-radius: var(--radius-full);
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
}

.media-detail__pill--favourite {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.media-detail__facts {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.media-detail__facts dt {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.media-detail__facts dd {
  margin: 0.25rem 0 0;
  font-weight: 600;
}

.media-detail__cover {
  width: 100%;
  aspect-ratio: 4 / 5;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  object-fit: cover;
  background: var(--color-surface-muted);
}

.media-detail__cover--placeholder {
  display: grid;
  place-items: center;
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.media-detail__layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.85fr);
}

.media-detail__main,
.media-detail__side {
  display: grid;
  gap: 1rem;
}

.media-detail__actions {
  margin-top: 1rem;
}

@media (max-width: 1080px) {
  .media-detail__hero,
  .media-detail__layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .media-detail__overview {
    grid-template-columns: 1fr;
  }
}
</style>
