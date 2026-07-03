<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

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
const profileReady = computed(
  () => matching.value?.interestProfile.isReadyForMatching ?? false,
);
const emptyState = computed(() => {
  if (!matching.value) {
    return {
      title: "Keine Match-Ergebnisse vorhanden",
      description: "Die Match-Daten sind gerade nicht verfuegbar.",
      to: { name: "candidates" as const },
      label: "Kandidaten ansehen",
    };
  }

  if (!profileReady.value) {
    return {
      title: "Profil noch nicht bereit fuer Matches",
      description: matching.value.interestProfile.explanationMessage,
      to: { name: "profile" as const },
      label: "Profil verbessern",
    };
  }

  return {
    title: "Noch keine Match-Ergebnisse vorhanden",
    description:
      "Lege Kandidaten mit dem Status 'Moechte ich konsumieren' an, damit MoodMatch sie mit deinem Profil vergleichen kann.",
    to: { name: "candidates" as const },
    label: "Kandidaten ansehen",
  };
});

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
          Warum diese Titel zu dir passen
        </h1>
        <p class="page-copy">
          Diese Seite zeigt die bereits passenden Empfehlungen mit Begruendung.
          Prozentwerte erscheinen bewusst nur dann, wenn genug Vergleichsbasis
          vorhanden ist.
        </p>
      </div>
    </header>

    <section
      v-if="!loading && !errorMessage"
      class="overview-stats"
    >
      <article class="page-card overview-stat-card">
        <p class="eyebrow">
          Kandidaten
        </p>
        <p class="overview-stat-card__value">
          {{ matching?.matches.length ?? 0 }}
        </p>
        <p class="overview-stat-card__copy">
          Aktuell ausgewertete Vorschlaege
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--success">
        <p class="eyebrow">
          Aussagekraeftige Scores
        </p>
        <p class="overview-stat-card__value">
          {{ meaningfulScoreCount }}
        </p>
        <p class="overview-stat-card__copy">
          Nur mit relativer Prozentangabe
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--warning">
        <p class="eyebrow">
          Noch nicht vergleichbar
        </p>
        <p class="overview-stat-card__value">
          {{ noScoreCount }}
        </p>
        <p class="overview-stat-card__copy">
          Davon {{ incompleteCount }} mit unvollstaendiger Datenbasis
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      class="matches-view__state-card"
      title="Matches werden geladen"
      description="Profil, Score-Hinweise und Kandidatenerklaerungen werden vorbereitet."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      class="matches-view__state-card"
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
        v-if="!profileReady"
        title="Dein Profil braucht noch mehr Signale"
        :description="matching.interestProfile.explanationMessage"
        tone="warning"
      >
        <div class="state-actions">
          <RouterLink
            :to="{ name: 'profile' }"
            class="button button--secondary"
          >
            Profil ansehen
          </RouterLink>
        </div>
      </AppMessage>

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
          ist noch nicht belastbar genug.
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
              {{ bestMatch.explanationMessage }}
            </p>
            <p class="body-muted matches-view__highlight-note">
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
        :title="emptyState.title"
        :description="emptyState.description"
      >
        <div class="state-actions">
          <RouterLink
            :to="emptyState.to"
            class="button button--primary"
          >
            {{ emptyState.label }}
          </RouterLink>
        </div>
      </AppMessage>

      <div
        v-else
        class="matches-view__list"
      >
        <div class="section-header">
          <div class="section-header__copy">
            <p class="eyebrow">
              Match-Karten
            </p>
            <h2 class="section-title">
              Bestaetigte Empfehlungen
            </h2>
            <p class="body-muted">
              Jede Karte verbindet Score, Gruende und Kandidatenbasis in einer
              kompakteren Ansicht.
            </p>
          </div>
        </div>

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
              title="Ausgangskandidat"
              compact
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
.matches-view__note-card,
.matches-view__highlight,
.matches-view__match-card {
  padding: 0.98rem;
}

.matches-view__hero {
  display: grid;
  gap: 0.9rem;
  grid-template-columns: minmax(320px, 1fr) minmax(300px, 0.9fr);
}

.matches-view__note-card {
  display: grid;
  gap: 0.35rem;
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--color-info-soft) 40%, var(--color-surface)),
    var(--color-surface)
  );
}

.matches-view__note-card p {
  margin: 0;
}

.matches-view__highlight {
  display: grid;
  gap: 0.72rem;
  align-content: start;
}

.matches-view__highlight-copy p {
  margin: 0.3rem 0 0;
}

.matches-view__highlight-note {
  font-size: 0.88rem;
}

.matches-view__list {
  display: grid;
  gap: 0.8rem;
}

.matches-view__state-card {
  width: min(100%, 58rem);
}

.matches-view__match-card {
  display: grid;
  gap: 0.85rem;
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
  gap: 0.9rem;
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
  .matches-view__hero {
    grid-template-columns: 1fr;
  }
}
</style>
