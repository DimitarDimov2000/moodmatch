<script setup lang="ts">
import { computed } from 'vue';

import FormField from '@/components/common/FormField.vue';
import {
  sourceHintForMediaType,
  sourceOptionsForMediaType,
  type ExternalSourceSelection,
} from '@/components/external/external-options';
import { mediaTypeOptions } from '@/components/media/media-options';
import type { MediaType } from '@/types/api';

const props = defineProps<{
  query: string;
  mediaType: MediaType;
  source: ExternalSourceSelection;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  'update:query': [value: string];
  'update:mediaType': [value: MediaType];
  'update:source': [value: ExternalSourceSelection];
  search: [];
}>();

const sourceOptions = computed(() => sourceOptionsForMediaType(props.mediaType));
const sourceHint = computed(() => sourceHintForMediaType(props.mediaType));
</script>

<template>
  <form
    class="external-search-form page-card"
    @submit.prevent="emit('search')"
  >
    <div class="external-search-form__grid">
      <FormField
        label="Suchbegriff"
        hint="Suche nach Filmen, Serien, Buechern oder Demo-Treffern und importiere sie direkt in deine Mediathek."
        required
      >
        <input
          :value="query"
          class="input"
          type="text"
          name="query"
          maxlength="200"
          placeholder="z. B. Arrival, Dark oder Dune"
          @input="emit('update:query', ($event.target as HTMLInputElement).value)"
        >
      </FormField>

      <FormField
        label="Medientyp"
        hint="Waehle zuerst den Bereich aus, damit MoodMatch die passenden Quellen anbieten kann."
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
        {{ submitting ? 'Suche laeuft...' : 'Externe Suche starten' }}
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
  grid-template-columns: minmax(0, 1.5fr) minmax(180px, 0.7fr) minmax(220px, 0.9fr);
}

.external-search-form__actions {
  display: flex;
  justify-content: flex-start;
}

@media (max-width: 720px) {
  .external-search-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
