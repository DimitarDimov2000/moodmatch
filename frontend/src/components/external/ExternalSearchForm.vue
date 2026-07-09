<script setup lang="ts">
import { computed } from 'vue';

import FormField from '@/components/common/FormField.vue';
import {
  getYouTubeSearchSortOptions,
  sourceHintForMediaType,
  sourceOptionsForMediaType,
  type ExternalSourceSelection,
} from '@/components/external/external-options';
import { getMediaTypeOptions } from '@/components/media/media-options';
import { i18n } from '@/i18n';
import type { ExternalSearchSort, MediaType } from '@/types/api';

const props = defineProps<{
  query: string;
  mediaType: MediaType;
  source: ExternalSourceSelection;
  sort: ExternalSearchSort;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  'update:query': [value: string];
  'update:mediaType': [value: MediaType];
  'update:source': [value: ExternalSourceSelection];
  'update:sort': [value: ExternalSearchSort];
  search: [];
}>();

const sourceOptions = computed(() => sourceOptionsForMediaType(props.mediaType));
const sourceHint = computed(() => sourceHintForMediaType(props.mediaType));
const showSort = computed(() => props.mediaType === 'VIDEO' && props.source === 'YOUTUBE');
const mediaTypeOptions = computed(() => getMediaTypeOptions());
const youTubeSearchSortOptions = computed(() => getYouTubeSearchSortOptions());
const { t } = i18n.global;
</script>

<template>
  <form
    class="external-search-form"
    @submit.prevent="emit('search')"
  >
    <div
      class="external-search-form__primary"
    >
      <FormField
        class="external-search-form__query-field"
        :label="t('externalSearch.query')"
        :hint="t('externalSearch.queryHint')"
        required
      >
        <input
          :value="query"
          class="input external-search-form__control"
          type="text"
          name="query"
          maxlength="200"
          :placeholder="t('externalSearch.queryPlaceholder')"
          @input="emit('update:query', ($event.target as HTMLInputElement).value)"
        >
      </FormField>
    </div>

    <div
      class="external-search-form__secondary"
      :class="{ 'external-search-form__secondary--with-sort': showSort }"
    >
      <FormField
        class="external-search-form__field"
        :label="t('externalSearch.mediaType')"
        :hint="t('externalSearch.mediaTypeHint')"
        required
      >
        <select
          :value="mediaType"
          class="input external-search-form__control"
          name="mediaType"
          @change="emit('update:mediaType', ($event.target as HTMLSelectElement).value as MediaType)"
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
        class="external-search-form__field"
        :label="t('externalSearch.source')"
        :hint="sourceHint"
        required
      >
        <select
          :value="source"
          class="input external-search-form__control"
          name="source"
          @change="emit('update:source', ($event.target as HTMLSelectElement).value as ExternalSourceSelection)"
        >
          <option
            v-for="option in sourceOptions"
            :key="option.value"
            :value="option.value"
            :disabled="option.disabled"
          >
            {{ option.label }}
          </option>
        </select>
      </FormField>

      <FormField
        v-if="showSort"
        class="external-search-form__field external-search-form__field--sort"
        :label="t('externalSearch.sort')"
        :hint="t('externalSearch.sortHint')"
        required
      >
        <select
          :value="sort"
          class="input external-search-form__control"
          name="sort"
          @change="emit('update:sort', ($event.target as HTMLSelectElement).value as ExternalSearchSort)"
        >
          <option
            v-for="option in youTubeSearchSortOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </FormField>

      <div class="external-search-form__actions">
        <button
          class="button button--primary external-search-form__submit"
          type="submit"
          :disabled="submitting || !query.trim()"
        >
          {{ submitting ? t('common.actions.loadingSearch') : t('common.actions.search') }}
        </button>
      </div>
    </div>
  </form>
</template>

<style scoped>
.external-search-form {
  --external-search-control-height: 2.8rem;
  --external-search-control-radius: calc(var(--radius-md) + 1px);
  display: grid;
  gap: 0.68rem;
}

.external-search-form__primary,
.external-search-form__query-field {
  min-width: 0;
}

.external-search-form__secondary {
  display: grid;
  gap: 0.68rem;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(7.25rem, auto);
  align-items: end;
}

.external-search-form__secondary--with-sort {
  grid-template-columns:
    minmax(0, 1fr)
    minmax(0, 1fr)
    minmax(0, 0.9fr)
    minmax(7.25rem, auto);
}

.external-search-form :deep(.form-field) {
  gap: 0.42rem;
  min-width: 0;
}

.external-search-form__control {
  min-height: var(--external-search-control-height);
  border-radius: var(--external-search-control-radius);
  font-size: 0.95rem;
}

.external-search-form__actions {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
}

.external-search-form__submit {
  min-width: 7.25rem;
  min-height: var(--external-search-control-height);
  padding-inline: 0.95rem;
  border-radius: var(--external-search-control-radius);
  font-size: 0.95rem;
}

@media (max-width: 720px) {
  .external-search-form__secondary,
  .external-search-form__secondary--with-sort {
    grid-template-columns: 1fr;
  }

  .external-search-form__actions {
    justify-content: stretch;
  }

  .external-search-form__submit {
    width: 100%;
  }

  .external-search-form__actions .button {
    width: 100%;
  }
}
</style>
