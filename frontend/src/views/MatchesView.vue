<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { ApiRequestError } from "@/api/client";
import { getMatches } from "@/api/matches";
import AppMessage from "@/components/common/AppMessage.vue";
import CandidateSummaryCard from "@/components/matching/CandidateSummaryCard.vue";
import MatchExplanation from "@/components/matching/MatchExplanation.vue";
import MatchScoreDisplay from "@/components/matching/MatchScoreDisplay.vue";
import InterestProfileWeights from "@/components/profile/InterestProfileWeights.vue";
import type { MatchResultResponse, MatchingResponse } from "@/types/api";

const matching = ref<MatchingResponse | null>(null);
const loading = ref(true);
const errorMessage = ref("");

const meaningfulScoreCount = computed(
  () =>
    matching.value?.matches.filter((item) => item.relativeScore !== null)
      .length ?? 0,
);

const incompleteCount = computed(
  () =>
    matching.value?.matches.filter(
      (item) => !item.candidate.isCompleteForMatching,
    ).length ?? 0,
);

const noScoreCount = computed(
  () =>
    matching.value?.matches.filter((item) => item.relativeScore === null)
      .length ?? 0,
);

const bestMatch = computed<MatchResultResponse | null>(
  () => matching.value?.matches[0] ?? null,
);

onMounted(async () => {
  await loadMatches();
});

async function loadMatches() {
  loading.value = true;
  errorMessage.value = "";

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

  return "Die Matches konnten nicht geladen werden.";
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
          Diese Ansicht zeigt Scores nur dann als Prozent, wenn das Backend sie
          als sinnvoll bewertet. Unzureichende Daten oder fehlende
          Vergleichbarkeit bleiben deshalb explizit sichtbar und werden nicht
          als 0 % dargestellt.
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
          Ohne Prozentangabe
        </p>
        <h2>{{ noScoreCount }}</h2>
        <p class="body-muted">
          Davon {{ incompleteCount }} mit unvollstaendiger Datenbasis
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

      <article
        v-else-if="noScoreCount > 0"
        class="page-card matches-view__note-card"
      >
        <p class="eyebrow">
          Einordnung
        </p>
        <h2 class="section-title">
          Keine Prozentangabe ist ein eigener Zustand
        </h2>
        <p class="body-muted">
          Ein Kandidat ohne Prozentzahl ist nicht automatisch schwach. Entweder
          fehlen Tags, es gibt keine Profilueberschneidung, oder der Vergleich
          ist mit den vorhandenen Kandidaten noch nicht belastbar genug.
        </p>
      </article>

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
          <div class="matches-view__match-header">
            <div>
              <p class="eyebrow">
                Match-Karte
              </p>
              <h2 class="section-title">
                {{ result.candidate.media.title }}
              </h2>
            </div>

            <MatchScoreDisplay
              :score="result.relativeScore"
              :suppressed="matching.scoresSuppressed"
            />
          </div>

          <div class="matches-view__match-top">
            <CandidateSummaryCard
              class="matches-view__candidate-card"
              :candidate="result.candidate"
              title="Kandidat im Vergleich"
              :show-expected-note="false"
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
.matches-view__note-card,
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

.matches-view__note-card {
  display: grid;
  gap: 0.45rem;
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--color-info-soft) 55%, var(--color-surface)),
    var(--color-surface)
  );
}

.matches-view__note-card p {
  margin: 0;
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

.matches-view__match-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.matches-view__match-header h2 {
  margin-top: 0.35rem;
}

.matches-view__match-top {
  display: grid;
  gap: 1.25rem;
  grid-template-columns: minmax(0, 1fr);
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
  .matches-view__hero {
    grid-template-columns: 1fr;
  }
}
</style>
