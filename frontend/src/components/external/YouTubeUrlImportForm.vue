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
    class="youtube-url-import-form"
    @submit.prevent="emit('resolve')"
  >
    <FormField
      class="youtube-url-import-form__field"
      :label="t('externalSearch.youTubeFieldLabel')"
      :hint="t('externalSearch.youTubeFieldHint')"
      required
    >
      <input
        :value="value"
        class="input youtube-url-import-form__control"
        type="text"
        name="youtubeUrl"
        maxlength="4000"
        :placeholder="t('externalSearch.youTubeFieldPlaceholder')"
        @input="emit('update:value', ($event.target as HTMLInputElement).value)"
      >
    </FormField>

    <div class="youtube-url-import-form__actions">
      <button
        class="button button--primary youtube-url-import-form__submit"
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
  --youtube-import-control-height: 2.8rem;
  --youtube-import-control-radius: calc(var(--radius-md) + 1px);
  display: grid;
  gap: 0.68rem;
  grid-template-columns: minmax(0, 1fr) minmax(7.25rem, auto);
  align-items: end;
}

.youtube-url-import-form__field {
  min-width: 0;
}

.youtube-url-import-form :deep(.form-field) {
  gap: 0.42rem;
}

.youtube-url-import-form__control {
  min-height: var(--youtube-import-control-height);
  border-radius: var(--youtube-import-control-radius);
  font-size: 0.95rem;
}

.youtube-url-import-form__actions {
  display: flex;
  justify-content: flex-end;
  align-items: stretch;
}

.youtube-url-import-form__submit {
  min-width: 7.25rem;
  min-height: var(--youtube-import-control-height);
  padding-inline: 0.95rem;
  border-radius: var(--youtube-import-control-radius);
  font-size: 0.95rem;
}

@media (max-width: 720px) {
  .youtube-url-import-form {
    grid-template-columns: 1fr;
  }

  .youtube-url-import-form__actions {
    justify-content: stretch;
  }

  .youtube-url-import-form__submit {
    width: 100%;
  }

  .youtube-url-import-form__actions .button {
    width: 100%;
  }
}
</style>
