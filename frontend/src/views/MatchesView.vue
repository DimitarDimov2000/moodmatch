<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { ApiRequestError } from '@/api/client';
import { getMatches } from '@/api/matches';
import AppMessage from '@/components/common/AppMessage.vue';
import CandidateSummaryCard from '@/components/matching/CandidateSummaryCard.vue';
import MatchExplanation from '@/components/matching/MatchExplanation.vue';
import MatchScoreDisplay from '@/components/matching/MatchScoreDisplay.vue';
import InterestProfileWeights from '@/components/profile/InterestProfileWeights.vue';
import type { MatchResultResponse, MatchingResponse } from '@/types/api';

const matching = ref<MatchingResponse | null>(null);
const loading = ref(true);
const errorMessage = ref('');

const meaningfulScoreCount = computed(
  () => matching.value?.matches.filter((item) => item.relativeScore !== null).length ?? 0,
);

const incompleteCount = computed(
  () => matching.value?.matches.filter((item) => !item.candidate.isCompleteForMatching).length ?? 0,
);

const bestMatch = computed<MatchResultResponse | null>(() => matching.value?.matches[0] ?? null);

onMounted(async () => {
  await loadMatches();
});

async function loadMatches() {
  loading.value = true;
  errorMessage.value = '';

  try {
    matching.value = await getMatches();
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

  return 'Die Matches konnten nicht geladen werden.';
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Matches
        </p>
        <h1 class="page-title">
          Erklaerbare Match-Ergebnisse
        </h1>
        <p class="page-copy">
          Diese Ansicht zeigt Scores nur dann als Prozent, wenn das Backend sie als sinnvoll bewertet.
          Unzureichende Daten oder fehlende Vergleichbarkeit erscheinen deshalb klar als eigener Zustand.
        </p>
      </div>
    </header>

    <section class="matches-view__summary">
      <article class="page-card matches-view__summary-card">
        <p class="eyebrow">
          Kandidaten
        </p>
        <h2>{{ matching?.matches.length ?? 0 }}</h2>
        <p class="body-muted">
          Aktuell ausgewertete Vorschlaege
        </p>
      </article>

      <article class="page-card matches-view__summary-card">
        <p class="eyebrow">
          Aussagekraeftige Scores
        </p>
        <h2>{{ meaningfulScoreCount }}</h2>
        <p class="body-muted">
          Nur mit relativer Prozentangabe
        </p>
      </article>

      <article class="page-card matches-view__summary-card">
        <p class="eyebrow">
          Unvollstaendige Kandidaten
        </p>
        <h2>{{ incompleteCount }}</h2>
        <p class="body-muted">
          Noch ohne vollstaendige Matching-Daten
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      title="Matches werden geladen"
      description="Profil, Score-Hinweise und Kandidatenerklaerungen werden vorbereitet."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      title="Matches konnten nicht geladen werden"
      :description="errorMessage"
      tone="error"
    >
      <div class="matches-view__message-actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadMatches"
        >
          Erneut versuchen
        </button>
      </div>
    </AppMessage>

    <template v-else-if="matching">
      <AppMessage
        v-if="matching.scoresSuppressed"
        title="Scores derzeit unterdrueckt"
        :description="matching.explanationMessage"
        tone="warning"
      />

      <section class="matches-view__hero">
        <InterestProfileWeights
          class="matches-view__weights"
          :tags="matching.interestProfile.weightedTags"
          title="Profilbasis fuer dieses Matching"
        />

        <article
          v-if="bestMatch"
          class="page-card matches-view__highlight"
        >
          <div class="matches-view__highlight-copy">
            <p class="eyebrow">
              Bester aktueller Treffer
            </p>
            <h2 class="section-title">
              {{ bestMatch.candidate.media.title }}
            </h2>
            <p class="body-muted">
              {{ matching.scoringMethodNote }}
            </p>
          </div>

          <MatchScoreDisplay
            :score="bestMatch.relativeScore"
            :suppressed="matching.scoresSuppressed"
            :insufficient-label="matching.explanationMessage"
          />
        </article>
      </section>

      <AppMessage
        v-if="matching.matches.length === 0"
        title="Keine Match-Ergebnisse vorhanden"
        description="Noch keine Kandidaten mit WANT_TO_CONSUME Status verfuegbar."
      />

      <div
        v-else
        class="matches-view__list"
      >
        <article
          v-for="result in matching.matches"
          :key="result.candidate.media.id"
          class="page-card matches-view__match-card"
        >
          <div class="matches-view__match-top">
            <CandidateSummaryCard
              class="matches-view__candidate-card"
              :candidate="result.candidate"
              title="Match-Kandidat"
              :show-expected-note="false"
            />

            <MatchScoreDisplay
              :score="result.relativeScore"
              :suppressed="matching.scoresSuppressed"
              :insufficient-label="result.explanationMessage"
            />
          </div>

          <MatchExplanation :result="result" />
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped>
.matches-view__summary {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.matches-view__summary-card,
.matches-view__highlight,
.matches-view__match-card {
  padding: 1.25rem;
}

.matches-view__summary-card h2,
.matches-view__summary-card p {
  margin: 0.35rem 0 0;
}

.matches-view__hero {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(320px, 1fr) minmax(300px, 0.9fr);
}

.matches-view__highlight {
  display: grid;
  gap: 1rem;
  align-content: start;
}

.matches-view__highlight-copy p {
  margin: 0.4rem 0 0;
}

.matches-view__list {
  display: grid;
  gap: 1rem;
}

.matches-view__match-card {
  display: grid;
  gap: 1.25rem;
}

.matches-view__match-top {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
}

.matches-view__candidate-card {
  padding: 0;
  border: 0;
  box-shadow: none;
  background: transparent;
}

.matches-view__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .matches-view__summary,
  .matches-view__hero,
  .matches-view__match-top {
    grid-template-columns: 1fr;
  }
}
</style>
