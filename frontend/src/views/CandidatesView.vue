<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { listCandidates } from '@/api/candidates';
import AppMessage from '@/components/common/AppMessage.vue';
import CandidateSummaryCard from '@/components/matching/CandidateSummaryCard.vue';
import type { CandidateMediaResponse } from '@/types/api';

const candidates = ref<CandidateMediaResponse[]>([]);
const loading = ref(true);
const errorMessage = ref('');

const completeCount = computed(() => candidates.value.filter((item) => item.isCompleteForMatching).length);
const incompleteCount = computed(() => candidates.value.length - completeCount.value);

onMounted(async () => {
  await loadCandidates();
});

async function loadCandidates() {
  loading.value = true;
  errorMessage.value = '';

  try {
    const response = await listCandidates();
    candidates.value = response.candidates;
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

  return 'Die Kandidaten konnten nicht geladen werden.';
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Kandidaten
        </p>
        <h1 class="page-title">
          Kandidaten fuer spaeteres Matching
        </h1>
        <p class="page-copy">
          Diese Liste zeigt nur vorhandene Kandidaten. Swipe- oder Entscheidungsmodi werden hier bewusst noch nicht umgesetzt.
        </p>
      </div>

      <div class="page-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--primary"
        >
          Kandidat anlegen
        </RouterLink>
      </div>
    </header>

    <section class="candidates-view__summary">
      <article class="page-card candidates-view__summary-card">
        <p class="eyebrow">
          Insgesamt
        </p>
        <h2>{{ candidates.length }}</h2>
        <p class="body-muted">
          WANT_TO_CONSUME Medien
        </p>
      </article>

      <article class="page-card candidates-view__summary-card">
        <p class="eyebrow">
          Matching bereit
        </p>
        <h2>{{ completeCount }}</h2>
        <p class="body-muted">
          Kandidaten mit erwarteten Tags
        </p>
      </article>

      <article class="page-card candidates-view__summary-card">
        <p class="eyebrow">
          Noch unvollstaendig
        </p>
        <h2>{{ incompleteCount }}</h2>
        <p class="body-muted">
          Ohne ausreichende Matching-Daten
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      title="Kandidaten werden geladen"
      description="Wir holen die aktuelle Kandidatenliste aus der typed API."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      title="Kandidaten konnten nicht geladen werden"
      :description="errorMessage"
      tone="error"
    >
      <div class="candidates-view__message-actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadCandidates"
        >
          Erneut versuchen
        </button>
      </div>
    </AppMessage>

    <AppMessage
      v-else-if="candidates.length === 0"
      title="Noch keine Medienvorschlaege vorhanden"
      description="Lege zuerst Kandidaten mit WANT_TO_CONSUME Status an. Externe Suche oder Swipe-Modi sind in diesem Schritt noch nicht Teil der UI."
    >
      <div class="candidates-view__message-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--primary"
        >
          Ersten Kandidaten anlegen
        </RouterLink>
      </div>
    </AppMessage>

    <div
      v-else
      class="candidates-view__list"
    >
      <CandidateSummaryCard
        v-for="candidate in candidates"
        :key="candidate.media.id"
        :candidate="candidate"
      />
    </div>
  </section>
</template>

<style scoped>
.candidates-view__summary {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.candidates-view__summary-card {
  padding: 1.25rem;
}

.candidates-view__summary-card h2,
.candidates-view__summary-card p {
  margin: 0.35rem 0 0;
}

.candidates-view__list {
  display: grid;
  gap: 1rem;
}

.candidates-view__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .candidates-view__summary {
    grid-template-columns: 1fr;
  }
}
</style>
