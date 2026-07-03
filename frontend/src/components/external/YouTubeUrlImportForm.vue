<script setup lang="ts">
import FormField from '@/components/common/FormField.vue';
import { i18n } from '@/i18n';

defineProps<{
  value: string;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: string];
  resolve: [];
}>();

const { t } = i18n.global;
</script>

<template>
  <form
    class="youtube-url-import-form page-card"
    @submit.prevent="emit('resolve')"
  >
    <div class="youtube-url-import-form__grid">
      <FormField
        :label="t('externalSearch.youTubeFieldLabel')"
        :hint="t('externalSearch.youTubeFieldHint')"
        required
      >
        <input
          :value="value"
          class="input"
          type="text"
          name="youtubeUrl"
          maxlength="4000"
          :placeholder="t('externalSearch.youTubeFieldPlaceholder')"
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
        {{ submitting ? t('common.actions.loadingPreview') : t('common.actions.preview') }}
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

@media (max-width: 720px) {
  .youtube-url-import-form__actions {
    justify-content: stretch;
  }

  .youtube-url-import-form__actions .button {
    width: 100%;
  }
}
</style>
