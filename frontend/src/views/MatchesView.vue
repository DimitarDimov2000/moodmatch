<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { getMatches } from "@/api/matches";
import AppMessage from "@/components/common/AppMessage.vue";
import CandidateSummaryCard from "@/components/matching/CandidateSummaryCard.vue";
import {
  getGeneratedMatchExplanation,
  getProfileReadinessSummary,
} from "@/components/matching/matching-copy";
import MatchExplanation from "@/components/matching/MatchExplanation.vue";
import MatchScoreDisplay from "@/components/matching/MatchScoreDisplay.vue";
import InterestProfileWeights from "@/components/profile/InterestProfileWeights.vue";
import { i18n } from "@/i18n";
import type { MatchResultResponse, MatchingResponse } from "@/types/api";

const matching = ref<MatchingResponse | null>(null);
const loading = ref(true);
const errorMessage = ref("");
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

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
const profileReadinessSummary = computed(() => {
  trackLocaleDependency();
  return matching.value ? getProfileReadinessSummary(matching.value.interestProfile) : "";
});
const bestMatchSummary = computed(() => {
  trackLocaleDependency();
  return bestMatch.value ? getGeneratedMatchExplanation(bestMatch.value) : "";
});
const emptyState = computed(() => {
  trackLocaleDependency();

  if (!matching.value) {
    return {
      title: t("matches.emptyUnavailableTitle"),
      description: t("matches.emptyUnavailableDescription"),
      to: { name: "candidates" as const },
      label: t("common.actions.openCandidates"),
    };
  }

  if (!profileReady.value) {
    return {
      title: t("matches.emptyProfileTitle"),
      description: profileReadinessSummary.value,
      to: { name: "profile" as const },
      label: t("matches.improveProfile"),
    };
  }

  return {
    title: t("matches.emptyUnavailableTitle"),
    description: t("matches.emptyCandidatesDescription"),
    to: { name: "candidates" as const },
    label: t("common.actions.openCandidates"),
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

  return t("matches.errorTitle");
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          {{ t("matches.eyebrow") }}
        </p>
        <h1 class="page-title">
          {{ t("matches.title") }}
        </h1>
        <p class="page-copy">
          {{ t("matches.intro") }}
        </p>
      </div>
    </header>

    <section
      v-if="!loading && !errorMessage"
      class="overview-stats"
    >
      <article class="page-card overview-stat-card">
        <p class="eyebrow">
          {{ t("matches.candidates") }}
        </p>
        <p class="overview-stat-card__value">
          {{ matching?.matches.length ?? 0 }}
        </p>
        <p class="overview-stat-card__copy">
          {{ t("matches.evaluatedSuggestions") }}
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--success">
        <p class="eyebrow">
          {{ t("matches.meaningfulScores") }}
        </p>
        <p class="overview-stat-card__value">
          {{ meaningfulScoreCount }}
        </p>
        <p class="overview-stat-card__copy">
          {{ t("matches.relativeOnly") }}
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--warning">
        <p class="eyebrow">
          {{ t("matches.notComparable") }}
        </p>
        <p class="overview-stat-card__value">
          {{ noScoreCount }}
        </p>
        <p class="overview-stat-card__copy">
          {{ t("matches.incompleteOfCount", { count: incompleteCount }) }}
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      class="matches-view__state-card"
      :title="t('matches.loadingTitle')"
      :description="t('matches.loadingDescription')"
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      class="matches-view__state-card"
      :title="t('matches.errorTitle')"
      :description="errorMessage"
      tone="error"
    >
      <div class="matches-view__message-actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadMatches"
        >
          {{ t("common.actions.retry") }}
        </button>
      </div>
    </AppMessage>

    <template v-else-if="matching">
      <AppMessage
        v-if="!profileReady"
        :title="t('matches.profileNeedsSignals')"
        :description="profileReadinessSummary"
        tone="warning"
      >
        <div class="state-actions">
          <RouterLink
            :to="{ name: 'profile' }"
            class="button button--secondary"
          >
            {{ t("common.actions.openProfile") }}
          </RouterLink>
        </div>
      </AppMessage>

      <AppMessage
        v-if="matching.scoresSuppressed"
        :title="t('matches.scoresSuppressed')"
        :description="t('matches.scoresSuppressedCopy')"
        tone="warning"
      />

      <article
        v-else-if="noScoreCount > 0"
        class="page-card matches-view__note-card"
      >
        <p class="eyebrow">
          {{ t("matches.noteEyebrow") }}
        </p>
        <h2 class="section-title">
          {{ t("matches.noteTitle") }}
        </h2>
        <p class="body-muted">
          {{ t("matches.noteCopy") }}
        </p>
      </article>

      <section class="matches-view__hero">
        <InterestProfileWeights
          class="matches-view__weights"
          :tags="matching.interestProfile.weightedTags"
          :title="t('matches.profileWeightsTitle')"
        />

        <article
          v-if="bestMatch"
          class="page-card matches-view__highlight"
        >
          <div class="matches-view__highlight-copy">
            <p class="eyebrow">
              {{ t("matches.highlight") }}
            </p>
            <h2 class="section-title">
              {{ bestMatch.candidate.media.title }}
            </h2>
            <p class="body-muted">
              {{ bestMatchSummary }}
            </p>
            <p class="body-muted matches-view__highlight-note">
              {{ t("matches.scoreMethodCopy") }}
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
              {{ t("matches.cardsEyebrow") }}
            </p>
            <h2 class="section-title">
              {{ t("matches.cardsTitle") }}
            </h2>
            <p class="body-muted">
              {{ t("matches.cardsCopy") }}
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
                {{ t("matches.cardEyebrow") }}
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
              :title="t('matches.baseCandidate')"
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
