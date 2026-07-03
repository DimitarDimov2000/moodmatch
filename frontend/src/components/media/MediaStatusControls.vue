<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import FormField from '@/components/common/FormField.vue';
import { i18n } from '@/i18n';
import {
  canBeFavourite,
  getConsumptionStatusOptions,
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
const { t } = i18n.global;

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
      return t('mediaStatus.validationConsumedRating');
    }

    if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
      return t('mediaStatus.validationRatingRange');
    }
  }

  if (leavingConsumed.value && !statusForm.confirmDestructiveChange) {
    return t('mediaStatus.validationConfirmDestructive');
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
          {{ t('mediaStatus.eyebrow') }}
        </p>
        <h2 class="section-title">
          {{ t('mediaStatus.title') }}
        </h2>
      </div>
      <button
        class="button"
        :class="props.media.isFavourite ? 'button--success' : 'button--secondary'"
        type="button"
        :disabled="pending || !canBeFavourite(media.consumptionStatus, media.rating)"
        @click="toggleFavourite"
      >
        {{ media.isFavourite ? t('mediaStatus.removeFavourite') : t('mediaStatus.setFavourite') }}
      </button>
    </div>

    <div class="status-controls__grid">
      <FormField :label="t('mediaForm.status')">
        <select
          v-model="statusForm.consumptionStatus"
          class="status-controls__input"
        >
          <option
            v-for="option in getConsumptionStatusOptions()"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </FormField>

      <FormField :label="t('mediaForm.rating')">
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
      <span>{{ t('mediaStatus.confirmLeavingConsumed') }}</span>
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
      <span>{{ t('mediaStatus.setFavouriteSameStep') }}</span>
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
      {{ t('mediaStatus.favouriteQuickHint') }}
    </p>

    <button
      class="button button--primary"
      type="button"
      :disabled="pending || Boolean(statusError)"
      @click="submitStatus"
    >
      {{ t('common.actions.saveStatus') }}
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
