<script setup lang="ts">
import { computed } from 'vue';

import FormField from '@/components/common/FormField.vue';
import {
  sourceHintForMediaType,
  sourceOptionsForMediaType,
  youTubeSearchSortOptions,
  type ExternalSourceSelection,
} from '@/components/external/external-options';
import { mediaTypeOptions } from '@/components/media/media-options';
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
</script>

<template>
  <form
    class="external-search-form page-card"
    @submit.prevent="emit('search')"
  >
    <div
      class="external-search-form__grid"
      :class="{ 'external-search-form__grid--with-sort': showSort }"
    >
      <FormField
        label="Suchbegriff"
        hint="Suche nach Titeln, Reihen, Autor:innen, Creator-Namen oder bekannten Schlagwoertern."
        required
      >
        <input
          :value="query"
          class="input"
          type="text"
          name="query"
          maxlength="200"
          placeholder="z. B. Arrival, Dark, Dune oder AI tutorial"
          @input="emit('update:query', ($event.target as HTMLInputElement).value)"
        >
      </FormField>

      <FormField
        label="Medientyp"
        hint="Der Medientyp legt fest, welche Quellen in dieser Suche verfuegbar sind."
        required
      >
        <select
          :value="mediaType"
          class="input"
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
        label="Quelle"
        :hint="sourceHint"
        required
      >
        <select
          :value="source"
          class="input"
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
        label="YouTube-Sortierung"
        hint="Gilt nur fuer die explizite YouTube-Suche."
        required
      >
        <select
          :value="sort"
          class="input"
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
    </div>

    <div class="external-search-form__actions">
      <button
        class="button button--primary"
        type="submit"
        :disabled="submitting || !query.trim()"
      >
        {{ submitting ? 'Suche laeuft...' : 'Jetzt suchen' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.external-search-form {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.external-search-form__grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1.7fr) minmax(170px, 0.8fr) minmax(220px, 1fr);
}

.external-search-form__grid--with-sort {
  grid-template-columns: minmax(0, 1.6fr) minmax(170px, 0.75fr) minmax(220px, 0.95fr) minmax(180px, 0.8fr);
}

.external-search-form__actions {
  display: flex;
  justify-content: flex-start;
}

@media (max-width: 720px) {
  .external-search-form__grid {
    grid-template-columns: 1fr;
  }

  .external-search-form__actions {
    justify-content: stretch;
  }

  .external-search-form__actions .button {
    width: 100%;
  }
}
</style>
