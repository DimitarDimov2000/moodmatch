<script setup lang="ts">
import FormField from '@/components/common/FormField.vue';
import { mediaTypeOptions } from '@/components/media/media-options';
import type { MediaType } from '@/types/api';

defineProps<{
  query: string;
  mediaType: MediaType;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  'update:query': [value: string];
  'update:mediaType': [value: MediaType];
  search: [];
}>();
</script>

<template>
  <form
    class="external-search-form page-card"
    @submit.prevent="emit('search')"
  >
    <div class="external-search-form__grid">
      <FormField
        label="Suchbegriff"
        hint="Filme und Serien nutzen TMDB, sobald ein API-Key im Backend gesetzt ist. Sonst faellt MoodMatch auf den Demo-Katalog zurueck."
        required
      >
        <input
          :value="query"
          class="input"
          type="text"
          maxlength="200"
          placeholder="z. B. Arrival, Dark oder Dune"
          @input="emit('update:query', ($event.target as HTMLInputElement).value)"
        >
      </FormField>

      <FormField
        label="Medientyp"
        hint="TMDB ist derzeit fuer Filme und Serien aktiv. Buecher und Games bleiben vorerst im Demo-Fallback."
        required
      >
        <select
          :value="mediaType"
          class="input"
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
  grid-template-columns: minmax(0, 1.6fr) minmax(220px, 0.8fr);
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
