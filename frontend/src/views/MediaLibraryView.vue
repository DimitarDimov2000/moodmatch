<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { listMedia } from '@/api/media';
import { listTags } from '@/api/tags';
import AppMessage from '@/components/common/AppMessage.vue';
import MediaCard from '@/components/media/MediaCard.vue';
import TagCategoryList from '@/components/tags/TagCategoryList.vue';
import type { MediaResponse, TagResponse } from '@/types/api';

const mediaItems = ref<MediaResponse[]>([]);
const tags = ref<TagResponse[]>([]);
const loading = ref(true);
const errorMessage = ref('');

const consumedCount = computed(
  () => mediaItems.value.filter((item) => item.consumptionStatus === 'CONSUMED').length,
);

onMounted(async () => {
  await loadPageData();
});

async function loadPageData() {
  loading.value = true;
  errorMessage.value = '';

  try {
    const [mediaResponse, tagResponse] = await Promise.all([listMedia(), listTags()]);
    mediaItems.value = mediaResponse;
    tags.value = tagResponse;
  } catch (error) {
    errorMessage.value = toUserMessage(error);
  } finally {
    loading.value = false;
  }
}

function toUserMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return 'Die Daten konnten gerade nicht geladen werden.';
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Media Library
        </p>
        <h1 class="page-title">
          Lokale Medien verwalten
        </h1>
        <p class="page-copy">
          Deine lokale Sammlung ist die Grundlage fuer Profilbildung und spaeteres Matching.
          Favoriten bleiben dabei ein eigenes Signal und bedeuten nicht Merken oder Zur Liste.
        </p>
      </div>

      <div class="page-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--primary"
        >
          Medium anlegen
        </RouterLink>
      </div>
    </header>

    <section class="media-library__summary">
      <article class="page-card media-library__summary-card">
        <p class="eyebrow">
          Sammlung
        </p>
        <h2>{{ mediaItems.length }}</h2>
        <p class="body-muted">
          Medien insgesamt
        </p>
      </article>

      <article class="page-card media-library__summary-card">
        <p class="eyebrow">
          Konsumiert
        </p>
        <h2>{{ consumedCount }}</h2>
        <p class="body-muted">
          Grundlage fuer Favoriten und Profilsignale
        </p>
      </article>

      <article class="page-card media-library__summary-card">
        <p class="eyebrow">
          Tags
        </p>
        <h2>{{ tags.length }}</h2>
        <p class="body-muted">
          Ueber die Tag API geladen
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      title="Mediendaten werden geladen"
      description="Liste und Tag-Kategorien werden vorbereitet."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      title="Ansicht konnte nicht geladen werden"
      :description="errorMessage"
      tone="error"
    >
      <div class="media-library__message-actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadPageData"
        >
          Erneut versuchen
        </button>
      </div>
    </AppMessage>

    <div
      v-else
      class="media-library__layout"
    >
      <section class="media-library__content">
        <AppMessage
          v-if="mediaItems.length === 0"
          title="Noch keine Medien angelegt"
          description="Lege zuerst konsumierte Medien oder Kandidaten an, damit MoodMatch spaeter sinnvoll vergleichen kann."
        >
          <div class="media-library__message-actions">
            <RouterLink
              :to="{ name: 'media-create' }"
              class="button button--primary"
            >
              Erstes Medium anlegen
            </RouterLink>
          </div>
        </AppMessage>

        <div
          v-else
          class="media-library__list"
        >
          <MediaCard
            v-for="media in mediaItems"
            :key="media.id"
            :media="media"
          />
        </div>
      </section>

      <TagCategoryList
        class="media-library__tags"
        :tags="tags"
        title="Verfuegbare Tags"
      />
    </div>
  </section>
</template>

<style scoped>
.media-library__summary {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.media-library__summary-card {
  padding: 1.25rem;
}

.media-library__summary-card h2,
.media-library__summary-card p {
  margin: 0.35rem 0 0;
}

.media-library__layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.9fr);
}

.media-library__content,
.media-library__list {
  display: grid;
  gap: 1rem;
}

.media-library__tags {
  align-self: start;
  position: sticky;
  top: var(--shell-sticky-offset);
}

.media-library__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .media-library__summary {
    grid-template-columns: 1fr;
  }

  .media-library__layout {
    grid-template-columns: 1fr;
  }

  .media-library__tags {
    position: static;
  }
}
</style>
