<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { listMedia } from "@/api/media";
import { listTags } from "@/api/tags";
import AppMessage from "@/components/common/AppMessage.vue";
import MediaCard from "@/components/media/MediaCard.vue";
import TagCategoryList from "@/components/tags/TagCategoryList.vue";
import type { MediaResponse, TagResponse } from "@/types/api";

const mediaItems = ref<MediaResponse[]>([]);
const tags = ref<TagResponse[]>([]);
const loading = ref(true);
const errorMessage = ref("");

const consumedCount = computed(
  () =>
    mediaItems.value.filter((item) => item.consumptionStatus === "CONSUMED")
      .length,
);
const taggedCount = computed(
  () => mediaItems.value.filter((item) => item.tags.length > 0).length,
);

onMounted(async () => {
  await loadPageData();
});

async function loadPageData() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const [mediaResponse, tagResponse] = await Promise.all([
      listMedia(),
      listTags(),
    ]);
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

  return "Die Daten konnten gerade nicht geladen werden.";
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
          Deine Mediathek
        </h1>
        <p class="page-copy">
          Deine Sammlung bleibt die Grundlage fuer Profilbildung und Matching.
          Hier siehst du konsumierte Medien und Kandidaten in einer kompakteren
          Uebersicht.
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

    <section
      v-if="!loading && !errorMessage"
      class="overview-stats"
    >
      <article class="page-card overview-stat-card">
        <p class="eyebrow">
          Sammlung
        </p>
        <p class="overview-stat-card__value">
          {{ mediaItems.length }}
        </p>
        <p class="overview-stat-card__copy">
          Medien insgesamt
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--success">
        <p class="eyebrow">
          Konsumiert
        </p>
        <p class="overview-stat-card__value">
          {{ consumedCount }}
        </p>
        <p class="overview-stat-card__copy">
          Grundlage fuer Profil und Favoriten
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--info">
        <p class="eyebrow">
          Tag-Basis
        </p>
        <p class="overview-stat-card__value">
          {{ taggedCount }}
        </p>
        <p class="overview-stat-card__copy">
          Medien mit bestaetigten Tags
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      class="media-library__state-card"
      title="Mediendaten werden geladen"
      description="Liste und Tag-Kategorien werden vorbereitet."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      class="media-library__state-card"
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
        <div class="section-header">
          <div class="section-header__copy">
            <p class="eyebrow">
              Sammlung
            </p>
            <h2 class="section-title">
              Angelegte Medien
            </h2>
            <p class="body-muted">
              Titel zuerst, danach Status, Herkunft und Tags fuer einen
              schnelleren Ueberblick.
            </p>
          </div>
        </div>

        <AppMessage
          v-if="mediaItems.length === 0"
          class="media-library__state-card"
          title="Noch keine Medien angelegt"
          description="Lege zuerst konsumierte Medien oder Kandidaten an, damit MoodMatch Profil, Sammlung und spaetere Vergleiche sinnvoll aufbauen kann."
        >
          <div class="state-actions">
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
        title="Tag-Kompass"
      />
    </div>
  </section>
</template>

<style scoped>
.media-library__layout {
  display: grid;
  gap: 0.95rem;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.9fr);
}

.media-library__content,
.media-library__list {
  display: grid;
  gap: 0.8rem;
}

.media-library__list {
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
}

.media-library__tags {
  align-self: start;
  position: sticky;
  top: var(--shell-sticky-offset);
}

.media-library__state-card {
  width: min(100%, 58rem);
}

.media-library__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .media-library__layout {
    grid-template-columns: 1fr;
  }

  .media-library__list {
    grid-template-columns: 1fr;
  }

  .media-library__tags {
    position: static;
  }
}
</style>
