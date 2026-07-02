<script setup lang="ts">
import FormField from '@/components/common/FormField.vue';

defineProps<{
  value: string;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: string];
  resolve: [];
}>();
</script>

<template>
  <form
    class="youtube-url-import-form page-card"
    @submit.prevent="emit('resolve')"
  >
    <div class="youtube-url-import-form__grid">
      <FormField
        label="YouTube-URL oder Video-ID"
        hint="Unterstuetzt watch-URLs, youtu.be-Links, Shorts-URLs und rohe Video-IDs. Die Aufloesung nutzt ausschliesslich die offizielle YouTube Data API."
        required
      >
        <input
          :value="value"
          class="input"
          type="text"
          name="youtubeUrl"
          maxlength="4000"
          placeholder="z. B. https://www.youtube.com/watch?v=... oder abc123XYZ_0"
          @input="emit('update:value', ($event.target as HTMLInputElement).value)"
        >
      </FormField>
    </div>

    <div class="youtube-url-import-form__actions">
      <button
        class="button button--primary"
        type="submit"
        :disabled="submitting || !value.trim()"
      >
        {{ submitting ? 'YouTube-Vorschau wird geladen...' : 'YouTube-Vorschau laden' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.youtube-url-import-form {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.youtube-url-import-form__grid {
  display: grid;
  gap: 1rem;
}

.youtube-url-import-form__actions {
  display: flex;
  justify-content: flex-start;
}
</style>
