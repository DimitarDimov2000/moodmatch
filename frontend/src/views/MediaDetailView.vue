<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import {
  deleteMedia,
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
const router = useRouter();
const media = ref<MediaResponse | null>(null);
const availableTags = ref<TagResponse[]>([]);
const loading = ref(true);
const saving = ref(false);
const deleting = ref(false);
const deleteConfirmationOpen = ref(false);
const errorMessage = ref('');
const deleteErrorMessage = ref('');
const apiErrors = ref<Record<string, string>>({});

const mediaId = computed(() => String(route.params.id));
const busy = computed(() => saving.value || deleting.value);

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
  deleteErrorMessage.value = '';
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
  deleteErrorMessage.value = '';

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
  deleteErrorMessage.value = '';

  try {
    media.value = await updateMediaFavourite(media.value.id, request);
  } catch (error) {
    errorMessage.value = toUserMessage(error);
  } finally {
    saving.value = false;
  }
}

function openDeleteConfirmation() {
  deleteConfirmationOpen.value = true;
  deleteErrorMessage.value = '';
}

function cancelDeleteConfirmation() {
  if (deleting.value) {
    return;
  }

  deleteConfirmationOpen.value = false;
  deleteErrorMessage.value = '';
}

async function confirmDelete() {
  if (!media.value) {
    return;
  }

  deleting.value = true;
  errorMessage.value = '';
  deleteErrorMessage.value = '';

  try {
    await deleteMedia(media.value.id);
    await router.push({ name: 'media-list' });
  } catch (error) {
    deleteErrorMessage.value = toDeleteUserMessage(error);
  } finally {
    deleting.value = false;
  }
}

function toUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return 'Die Daten konnten nicht verarbeitet werden.';
}

function toDeleteUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return 'Das Medium konnte nicht geloescht werden.';
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
          :pending="busy"
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
            :submitting="busy"
            :api-errors="apiErrors"
            submit-label="Aenderungen speichern"
            @submit="handleSubmit"
          />

          <section class="page-card media-detail__danger-zone">
            <div class="media-detail__danger-copy">
              <p class="eyebrow media-detail__danger-eyebrow">
                Danger Zone
              </p>
              <h2 class="section-title">
                Medium dauerhaft loeschen
              </h2>
              <p class="body-muted">
                Diese Aktion ist bewusst von normalen Bearbeitungen getrennt und entfernt das Medium aus deiner lokalen Sammlung.
              </p>
            </div>

            <AppMessage
              v-if="deleteErrorMessage"
              title="Loeschen fehlgeschlagen"
              :description="deleteErrorMessage"
              tone="error"
            />

            <div
              v-if="deleteConfirmationOpen"
              class="media-detail__danger-confirm"
            >
              <p class="media-detail__danger-question">
                Wirklich <strong>{{ media.title }}</strong> loeschen?
              </p>
              <p class="body-muted">
                Nach erfolgreichem Loeschen wechselst du zur Medienliste zurueck.
              </p>

              <div class="page-actions">
                <button
                  class="button button--secondary"
                  type="button"
                  :disabled="deleting"
                  @click="cancelDeleteConfirmation"
                >
                  Abbrechen
                </button>
                <button
                  class="button button--danger"
                  type="button"
                  :disabled="deleting"
                  @click="confirmDelete"
                >
                  {{ deleting ? 'Wird geloescht...' : 'Loeschen bestaetigen' }}
                </button>
              </div>
            </div>

            <div
              v-else
              class="media-detail__danger-actions"
            >
              <button
                class="button button--danger"
                type="button"
                :disabled="busy"
                @click="openDeleteConfirmation"
              >
                Medium loeschen
              </button>
            </div>
          </section>
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

.media-detail__danger-zone {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
  border-color: color-mix(in srgb, var(--color-error) 30%, var(--color-border));
  background: color-mix(in srgb, var(--color-error-soft) 35%, var(--color-surface));
}

.media-detail__danger-copy p,
.media-detail__danger-question {
  margin: 0.45rem 0 0;
}

.media-detail__danger-eyebrow {
  color: var(--color-error);
}

.media-detail__danger-actions,
.media-detail__danger-confirm {
  display: grid;
  gap: 0.85rem;
}

.media-detail__danger-question {
  font-weight: 600;
  color: var(--color-text-primary);
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
