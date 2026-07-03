<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { createMedia, replaceMediaTags } from '@/api/media';
import { listTags } from '@/api/tags';
import AppMessage from '@/components/common/AppMessage.vue';
import { i18n } from '@/i18n';
import MediaForm, { type MediaFormSubmitPayload } from '@/components/media/MediaForm.vue';
import type { TagResponse } from '@/types/api';

const router = useRouter();
const availableTags = ref<TagResponse[]>([]);
const loading = ref(true);
const submitting = ref(false);
const pageError = ref('');
const apiErrors = ref<Record<string, string>>({});

const hasTags = computed(() => availableTags.value.length > 0);
const { t } = i18n.global;

onMounted(async () => {
  try {
    availableTags.value = await listTags();
  } catch (error) {
    pageError.value = toUserMessage(error);
  } finally {
    loading.value = false;
  }
});

async function handleSubmit(payload: MediaFormSubmitPayload) {
  submitting.value = true;
  pageError.value = '';
  apiErrors.value = {};

  try {
    const createdMedia = await createMedia(payload.media);

    if (payload.tagIds.length > 0) {
      await replaceMediaTags(createdMedia.id, { tagIds: payload.tagIds });
    }

    await router.push({ name: 'media-detail', params: { id: createdMedia.id } });
  } catch (error) {
    if (error instanceof ApiRequestError) {
      pageError.value = error.message;
      apiErrors.value = Object.fromEntries(
        error.details
          .filter((detail) => detail.field)
          .map((detail) => [detail.field as string, detail.message]),
      );
    } else {
      pageError.value = t('mediaCreate.createError');
    }
  } finally {
    submitting.value = false;
  }
}

function toUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return t('mediaCreate.tagsError');
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          {{ t("mediaCreate.eyebrow") }}
        </p>
        <h1 class="page-title">
          {{ t("mediaCreate.title") }}
        </h1>
        <p class="page-copy">
          {{ t("mediaCreate.intro") }}
        </p>
      </div>
    </header>

    <AppMessage
      v-if="loading"
      :title="t('mediaCreate.loadingTitle')"
      :description="t('mediaCreate.loadingDescription')"
      tone="info"
    />

    <AppMessage
      v-else-if="pageError && !hasTags"
      :title="t('mediaCreate.unavailableTitle')"
      :description="pageError"
      tone="error"
    />

    <template v-else>
      <AppMessage
        v-if="pageError"
        :title="t('mediaCreate.saveFailed')"
        :description="pageError"
        tone="error"
      />

      <MediaForm
        mode="create"
        :available-tags="availableTags"
        :submitting="submitting"
        :api-errors="apiErrors"
        @submit="handleSubmit"
      />
    </template>
  </section>
</template>
