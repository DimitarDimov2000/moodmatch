<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { listCandidates } from "@/api/candidates";
import AppMessage from "@/components/common/AppMessage.vue";
import CandidateSummaryCard from "@/components/matching/CandidateSummaryCard.vue";
import type { CandidateMediaResponse } from "@/types/api";

const candidates = ref<CandidateMediaResponse[]>([]);
const loading = ref(true);
const errorMessage = ref("");

const completeCount = computed(
  () => candidates.value.filter((item) => item.isCompleteForMatching).length,
);
const incompleteCount = computed(
  () => candidates.value.length - completeCount.value,
);

onMounted(async () => {
  await loadCandidates();
});

async function loadCandidates() {
  loading.value = true;
  errorMessage.value = "";

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

  return "Die Kandidaten konnten nicht geladen werden.";
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
          Kandidaten fuer dein naechstes Match
        </h1>
        <p class="page-copy">
          Diese Liste zeigt moegliche Empfehlungen vor der eigentlichen
          Entscheidung. Du siehst sofort, welche Kandidaten schon vergleichbar
          sind und wo noch erwartete Signale fehlen.
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

    <section
      v-if="!loading && !errorMessage"
      class="overview-stats"
    >
      <article class="page-card overview-stat-card">
        <p class="eyebrow">
          Insgesamt
        </p>
        <p class="overview-stat-card__value">
          {{ candidates.length }}
        </p>
        <p class="overview-stat-card__copy">
          WANT_TO_CONSUME Medien
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--success">
        <p class="eyebrow">
          Matching bereit
        </p>
        <p class="overview-stat-card__value">
          {{ completeCount }}
        </p>
        <p class="overview-stat-card__copy">
          Kandidaten mit erwarteten Tags
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--warning">
        <p class="eyebrow">
          Braucht Pflege
        </p>
        <p class="overview-stat-card__value">
          {{ incompleteCount }}
        </p>
        <p class="overview-stat-card__copy">
          Ohne ausreichende Matching-Daten
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      class="candidates-view__state-card"
      title="Kandidaten werden geladen"
      description="Wir holen die aktuelle Kandidatenliste aus der typed API."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      class="candidates-view__state-card"
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
      class="candidates-view__state-card"
      title="Noch keine Kandidaten fuer spaetere Empfehlungen"
      description="Lege WANT_TO_CONSUME Medien an, damit hier die Vorschlaege erscheinen, die MoodMatch als naechste Vergleichsliste vorbereitet."
    >
      <div class="state-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--primary"
        >
          Ersten Kandidaten anlegen
        </RouterLink>
      </div>
    </AppMessage>

    <section
      v-else
      class="candidates-view__content"
    >
      <div class="section-header">
        <div class="section-header__copy">
          <p class="eyebrow">
            Vergleichsliste
          </p>
          <h2 class="section-title">
            Kandidaten vor der Entscheidung
          </h2>
          <p class="body-muted">
            Die Karten bleiben bewusst kompakt und vergleichbar. Vollstaendige
            Kandidaten sind bereit fuer spaetere Matches, unvollstaendige zeigen
            knapp, was noch fehlt.
          </p>
        </div>
      </div>

      <div class="candidates-view__list">
        <CandidateSummaryCard
          v-for="candidate in candidates"
          :key="candidate.media.id"
          :candidate="candidate"
        />
      </div>
    </section>
  </section>
</template>

<style scoped>
.candidates-view__content {
  display: grid;
  gap: 0.8rem;
}

.candidates-view__list {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.candidates-view__state-card {
  width: min(100%, 58rem);
}

.candidates-view__message-actions {
  margin-top: 1rem;
}

@media (max-width: 760px) {
  .candidates-view__list {
    grid-template-columns: 1fr;
  }
}
</style>
