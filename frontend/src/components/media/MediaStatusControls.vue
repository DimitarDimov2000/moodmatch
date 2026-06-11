<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import FormField from '@/components/common/FormField.vue';
import {
  canBeFavourite,
  consumptionStatusOptions,
} from '@/components/media/media-options';
import type {
  MediaResponse,
  UpdateMediaConsumptionStatusRequest,
  UpdateMediaFavouriteRequest,
} from '@/types/api';

const props = defineProps<{
  media: MediaResponse;
  pending?: boolean;
}>();

const emit = defineEmits<{
  updateStatus: [request: UpdateMediaConsumptionStatusRequest];
  updateFavourite: [request: UpdateMediaFavouriteRequest];
}>();

const statusForm = reactive({
  consumptionStatus: props.media.consumptionStatus,
  rating: props.media.rating === null ? '' : String(props.media.rating),
  isFavourite: props.media.isFavourite,
  confirmDestructiveChange: false,
});

watch(
  () => props.media,
  (media) => {
    statusForm.consumptionStatus = media.consumptionStatus;
    statusForm.rating = media.rating === null ? '' : String(media.rating);
    statusForm.isFavourite = media.isFavourite;
    statusForm.confirmDestructiveChange = false;
  },
  { deep: true },
);

const leavingConsumed = computed(
  () => props.media.consumptionStatus === 'CONSUMED' && statusForm.consumptionStatus !== 'CONSUMED',
);

const favouriteAllowed = computed(() =>
  canBeFavourite(statusForm.consumptionStatus, statusForm.rating ? Number(statusForm.rating) : null),
);

const statusError = computed(() => {
  if (statusForm.consumptionStatus === 'CONSUMED') {
    const rating = Number(statusForm.rating);

    if (!statusForm.rating) {
      return 'Beim Status konsumiert ist eine Bewertung erforderlich.';
    }

    if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
      return 'Bewertung muss eine ganze Zahl von 1 bis 5 sein.';
    }
  }

  if (leavingConsumed.value && !statusForm.confirmDestructiveChange) {
    return 'Bitte bestaetigen, dass Bewertung und Favorit beim Statuswechsel entfernt werden duerfen.';
  }

  return '';
});

function submitStatus() {
  if (statusError.value) {
    return;
  }

  emit('updateStatus', {
    consumptionStatus: statusForm.consumptionStatus,
    rating: statusForm.consumptionStatus === 'CONSUMED' ? Number(statusForm.rating) : null,
    isFavourite: favouriteAllowed.value ? statusForm.isFavourite : false,
    confirmDestructiveChange: leavingConsumed.value ? statusForm.confirmDestructiveChange : false,
  });
}

function toggleFavourite() {
  emit('updateFavourite', {
    isFavourite: !props.media.isFavourite,
  });
}
</script>

<template>
  <section class="status-controls page-card">
    <div class="status-controls__header">
      <div>
        <p class="eyebrow">
          Schnellaktionen
        </p>
        <h2 class="section-title">
          Status und Favorit
        </h2>
      </div>
      <button
        class="button"
        :class="props.media.isFavourite ? 'button--success' : 'button--secondary'"
        type="button"
        :disabled="pending || !canBeFavourite(media.consumptionStatus, media.rating)"
        @click="toggleFavourite"
      >
        {{ media.isFavourite ? 'Favorit entfernen' : 'Als Favorit markieren' }}
      </button>
    </div>

    <div class="status-controls__grid">
      <FormField label="Status">
        <select
          v-model="statusForm.consumptionStatus"
          class="status-controls__input"
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

      <FormField label="Bewertung">
        <input
          v-model="statusForm.rating"
          class="status-controls__input"
          type="number"
          min="1"
          max="5"
          step="1"
          :disabled="statusForm.consumptionStatus !== 'CONSUMED'"
        >
      </FormField>
    </div>

    <label
      v-if="leavingConsumed"
      class="status-controls__confirm"
    >
      <input
        v-model="statusForm.confirmDestructiveChange"
        type="checkbox"
      >
      <span>Ich bestaetige, dass Bewertung und Favorit entfernt werden duerfen.</span>
    </label>

    <label
      class="status-controls__confirm"
      :class="{ 'status-controls__confirm--disabled': !favouriteAllowed }"
    >
      <input
        v-model="statusForm.isFavourite"
        type="checkbox"
        :disabled="!favouriteAllowed"
      >
      <span>Favorit im selben Schritt setzen</span>
    </label>

    <p
      v-if="statusError"
      class="status-controls__error"
    >
      {{ statusError }}
    </p>

    <p
      v-if="!canBeFavourite(media.consumptionStatus, media.rating)"
      class="status-controls__hint"
    >
      Die Favoriten-Schnellaktion ist nur fuer konsumierte Medien mit Bewertung 4 oder 5 verfuegbar.
    </p>

    <button
      class="button button--primary"
      type="button"
      :disabled="pending || Boolean(statusError)"
      @click="submitStatus"
    >
      Status aktualisieren
    </button>
  </section>
</template>

<style scoped>
.status-controls {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.status-controls__header {
  display: flex;
  flex-wrap: wrap;
  align-items: start;
  justify-content: space-between;
  gap: 1rem;
}

.status-controls__grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.status-controls__input {
  width: 100%;
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.status-controls__confirm {
  display: flex;
  align-items: start;
  gap: 0.75rem;
  color: var(--color-text-secondary);
}

.status-controls__confirm--disabled {
  color: var(--color-text-muted);
}

.status-controls__error {
  margin: 0;
  color: var(--color-error);
}

.status-controls__hint {
  margin: 0;
  color: var(--color-text-muted);
}

@media (max-width: 680px) {
  .status-controls__grid {
    grid-template-columns: 1fr;
  }
}
</style>

