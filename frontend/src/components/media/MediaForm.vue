<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import FormField from '@/components/common/FormField.vue';
import {
  canBeFavourite,
  commitmentLevelOptions,
  consumptionStatusLabels,
  consumptionStatusOptions,
  mediaTypeOptions,
  sourceTypeOptions,
} from '@/components/media/media-options';
import TagChip from '@/components/tags/TagChip.vue';
import type {
  CreateMediaRequest,
  MediaResponse,
  TagResponse,
  UpdateMediaRequest,
} from '@/types/api';

export interface MediaFormSubmitPayload {
  media: CreateMediaRequest | UpdateMediaRequest;
  tagIds: string[];
}

interface MediaFormModel {
  title: string;
  originalTitle: string;
  description: string;
  mediaType: CreateMediaRequest['mediaType'];
  consumptionStatus: CreateMediaRequest['consumptionStatus'];
  isFavourite: boolean;
  rating: number | null;
  sourceType: CreateMediaRequest['sourceType'];
  sourceNote: string;
  commitmentLevel: CreateMediaRequest['commitmentLevel'];
  releaseYear: number | null;
  coverUrl: string;
  metadataOrigin: NonNullable<CreateMediaRequest['metadataOrigin']>;
  selectedTagIds: string[];
}

const props = defineProps<{
  mode: 'create' | 'edit';
  availableTags: TagResponse[];
  submitting?: boolean;
  initialMedia?: MediaResponse | null;
  submitLabel?: string;
  apiErrors?: Record<string, string>;
}>();

const emit = defineEmits<{
  submit: [payload: MediaFormSubmitPayload];
}>();

const model = reactive<MediaFormModel>(createModel(props.initialMedia ?? null));

watch(
  () => props.initialMedia,
  (media) => {
    Object.assign(model, createModel(media ?? null));
  },
);

watch(
  () => model.consumptionStatus,
  (status) => {
    if (status !== 'CONSUMED') {
      model.rating = null;
      model.isFavourite = false;
    }
  },
);

watch(
  () => model.rating,
  (rating) => {
    if (!canBeFavourite(model.consumptionStatus, rating)) {
      model.isFavourite = false;
    }
  },
);

const localErrors = computed<Record<string, string>>(() => {
  const errors: Record<string, string> = {};

  if (!model.title.trim()) {
    errors.title = 'Titel ist erforderlich.';
  }

  if (model.consumptionStatus === 'CONSUMED' && model.rating === null) {
    errors.rating = 'Bei konsumierten Medien ist eine Bewertung erforderlich.';
  }

  if (
    model.rating !== null &&
    (!Number.isInteger(model.rating) || model.rating < 1 || model.rating > 5)
  ) {
    errors.rating = 'Bewertung muss eine ganze Zahl von 1 bis 5 sein.';
  }

  if (
    model.releaseYear !== null &&
    (!Number.isInteger(model.releaseYear) || model.releaseYear < 1800 || model.releaseYear > 3000)
  ) {
    errors.releaseYear = 'Bitte ein plausibles Erscheinungsjahr eingeben.';
  }

  if (model.isFavourite && !canBeFavourite(model.consumptionStatus, model.rating)) {
    errors.isFavourite = 'Favorit ist nur fuer konsumierte Medien mit Bewertung 4 oder 5 moeglich.';
  }

  return errors;
});

const mergedErrors = computed(() => ({
  ...props.apiErrors,
  ...localErrors.value,
}));

const canSubmit = computed(() => Object.keys(localErrors.value).length === 0);
const showRating = computed(() => model.consumptionStatus === 'CONSUMED');
const favouriteEnabled = computed(() => canBeFavourite(model.consumptionStatus, model.rating));

function toggleTag(tagId: string) {
  if (model.selectedTagIds.includes(tagId)) {
    model.selectedTagIds = model.selectedTagIds.filter((currentId) => currentId !== tagId);
    return;
  }

  model.selectedTagIds = [...model.selectedTagIds, tagId];
}

function submitForm() {
  if (!canSubmit.value) {
    return;
  }

  emit('submit', {
    media: {
      title: model.title.trim(),
      originalTitle: normalizeOptionalText(model.originalTitle),
      description: normalizeOptionalText(model.description),
      mediaType: model.mediaType,
      consumptionStatus: model.consumptionStatus,
      isFavourite: favouriteEnabled.value ? model.isFavourite : false,
      rating: showRating.value ? model.rating : null,
      sourceType: model.sourceType,
      sourceNote: normalizeOptionalText(model.sourceNote),
      commitmentLevel: model.commitmentLevel,
      releaseYear: model.releaseYear,
      coverUrl: normalizeOptionalText(model.coverUrl),
      metadataOrigin: model.metadataOrigin,
    },
    tagIds: model.selectedTagIds,
  });
}

function createModel(media: MediaResponse | null): MediaFormModel {
  return {
    title: media?.title ?? '',
    originalTitle: media?.originalTitle ?? '',
    description: media?.description ?? '',
    mediaType: media?.mediaType ?? 'FILM',
    consumptionStatus: media?.consumptionStatus ?? 'WANT_TO_CONSUME',
    isFavourite: media?.isFavourite ?? false,
    rating: media?.rating ?? null,
    sourceType: media?.sourceType ?? 'MANUAL',
    sourceNote: media?.sourceNote ?? '',
    commitmentLevel: media?.commitmentLevel ?? 'MEDIUM',
    releaseYear: media?.releaseYear ?? null,
    coverUrl: media?.coverUrl ?? '',
    metadataOrigin: media?.metadataOrigin ?? 'MANUAL',
    selectedTagIds: media?.tags.map((tag) => tag.id) ?? [],
  };
}

function normalizeOptionalText(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function parseNullableInteger(value: string): number | null {
  const trimmed = value.trim();

  if (!trimmed) {
    return null;
  }

  const parsed = Number(trimmed);

  return Number.isNaN(parsed) ? null : parsed;
}

function handleRatingInput(event: Event) {
  const input = event.target as HTMLInputElement;
  model.rating = parseNullableInteger(input.value);
}

function handleReleaseYearInput(event: Event) {
  const input = event.target as HTMLInputElement;
  model.releaseYear = parseNullableInteger(input.value);
}
</script>

<template>
  <form
    class="media-form page-card"
    @submit.prevent="submitForm"
  >
    <div class="media-form__grid">
      <FormField
        label="Titel"
        required
        :error="mergedErrors.title"
      >
        <input
          v-model="model.title"
          name="title"
          class="media-form__input"
          type="text"
          placeholder="z. B. Arrival"
        >
      </FormField>

      <FormField
        label="Originaltitel"
        :error="mergedErrors.originalTitle"
      >
        <input
          v-model="model.originalTitle"
          name="originalTitle"
          class="media-form__input"
          type="text"
          placeholder="Optional"
        >
      </FormField>

      <FormField
        label="Medientyp"
        required
        :error="mergedErrors.mediaType"
      >
        <select
          v-model="model.mediaType"
          name="mediaType"
          class="media-form__input"
        >
          <option
            v-for="option in mediaTypeOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </FormField>

      <FormField
        label="Status"
        required
        :hint="mode === 'edit' ? 'Fuer reine Statuswechsel steht unten auch eine Schnellaktion bereit.' : undefined"
        :error="mergedErrors.consumptionStatus"
      >
        <select
          v-model="model.consumptionStatus"
          name="consumptionStatus"
          class="media-form__input"
        >
          <option
            v-for="option in consumptionStatusOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </FormField>

      <FormField
        label="Bewertung"
        hint="Nur fuer konsumierte Medien, 1 bis 5."
        :error="mergedErrors.rating"
      >
        <input
          :value="model.rating ?? ''"
          name="rating"
          class="media-form__input"
          type="number"
          min="1"
          max="5"
          step="1"
          :disabled="!showRating"
          placeholder="1-5"
          @input="handleRatingInput"
        >
      </FormField>

      <FormField
        label="Favorit"
        hint="Nur bei konsumiert und Bewertung 4 oder 5."
        :error="mergedErrors.isFavourite"
      >
        <label class="media-form__checkbox">
          <input
            v-model="model.isFavourite"
            name="isFavourite"
            type="checkbox"
            :disabled="!favouriteEnabled"
          >
          <span>
            Als Favorit markieren
            <small>
              {{ consumptionStatusLabels[model.consumptionStatus] }}
            </small>
          </span>
        </label>
      </FormField>

      <FormField
        label="Quelle"
        required
        :error="mergedErrors.sourceType"
      >
        <select
          v-model="model.sourceType"
          name="sourceType"
          class="media-form__input"
        >
          <option
            v-for="option in sourceTypeOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </FormField>

      <FormField
        label="Quelle Notiz"
        :error="mergedErrors.sourceNote"
      >
        <input
          v-model="model.sourceNote"
          name="sourceNote"
          class="media-form__input"
          type="text"
          placeholder="Optional"
        >
      </FormField>

      <FormField
        label="Umfang"
        required
        :error="mergedErrors.commitmentLevel"
      >
        <select
          v-model="model.commitmentLevel"
          name="commitmentLevel"
          class="media-form__input"
        >
          <option
            v-for="option in commitmentLevelOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </FormField>

      <FormField
        label="Erscheinungsjahr"
        :error="mergedErrors.releaseYear"
      >
        <input
          :value="model.releaseYear ?? ''"
          name="releaseYear"
          class="media-form__input"
          type="number"
          min="1800"
          max="3000"
          step="1"
          placeholder="Optional"
          @input="handleReleaseYearInput"
        >
      </FormField>

      <FormField
        label="Cover URL"
        :error="mergedErrors.coverUrl"
      >
        <input
          v-model="model.coverUrl"
          name="coverUrl"
          class="media-form__input"
          type="url"
          placeholder="https://..."
        >
      </FormField>
    </div>

    <FormField
      label="Beschreibung"
      :error="mergedErrors.description"
    >
      <textarea
        v-model="model.description"
        name="description"
        class="media-form__input media-form__input--textarea"
        rows="5"
        placeholder="Kurz beschreiben, worum es geht oder warum das Medium fuer dich relevant ist."
      />
    </FormField>

    <section class="media-form__tag-section">
      <div class="media-form__tag-header">
        <h2 class="section-title">
          Tags
        </h2>
        <p class="body-muted">
          Hilfreiche Zuordnung fuer spaetere Profil- und Matchinglogik. Die Regeln selbst bleiben im Backend.
        </p>
      </div>

      <div
        v-if="availableTags.length"
        class="media-form__tag-list"
      >
        <button
          v-for="tag in availableTags"
          :key="tag.id"
          class="media-form__tag-button"
          type="button"
          @click="toggleTag(tag.id)"
        >
          <TagChip
            :tag="tag"
            :selected="model.selectedTagIds.includes(tag.id)"
          />
        </button>
      </div>
      <p
        v-else
        class="body-muted"
      >
        Noch keine Tags verfuegbar. Du kannst das Medium trotzdem jetzt speichern und Tags spaeter hinzufuegen, sobald welche angelegt oder importiert wurden.
      </p>
    </section>

    <div class="media-form__footer">
      <p class="body-muted">
        Pflichtfelder sind markiert. Zusatzhinweise unterstuetzen nur die Eingabe, die verbindlichen Regeln prueft das Backend.
      </p>
      <button
        class="button button--primary"
        type="submit"
        :disabled="submitting || !canSubmit"
      >
        {{ submitLabel ?? (mode === 'create' ? 'Medium anlegen' : 'Aenderungen speichern') }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.media-form {
  display: grid;
  gap: 1.5rem;
  padding: 1.5rem;
}

.media-form__grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.media-form__input {
  width: 100%;
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.media-form__input:focus {
  outline: 2px solid color-mix(in srgb, var(--color-accent) 35%, transparent);
  outline-offset: 1px;
  border-color: var(--color-accent);
}

.media-form__input:disabled {
  background: var(--color-surface-secondary);
}

.media-form__input--textarea {
  resize: vertical;
}

.media-form__checkbox {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.media-form__checkbox small {
  display: block;
  margin-top: 0.15rem;
  color: var(--color-text-muted);
}

.media-form__tag-section {
  display: grid;
  gap: 1rem;
}

.media-form__tag-header p {
  margin: 0.5rem 0 0;
}

.media-form__tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.media-form__tag-button {
  padding: 0;
  background: transparent;
}

.media-form__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.media-form__footer p {
  margin: 0;
  max-width: 42rem;
}

@media (max-width: 780px) {
  .media-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
