<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { getMatches } from "@/api/matches";
import { getDisplayText } from "@/components/media/media-presentation";
import AppMessage from "@/components/common/AppMessage.vue";
import CandidateSummaryCard from "@/components/matching/CandidateSummaryCard.vue";
import {
  getGeneratedMatchExplanation,
  getProfileReadinessSummary,
} from "@/components/matching/matching-copy";
import MatchExplanation from "@/components/matching/MatchExplanation.vue";
import {
  getScoreTone,
  mediaTypeLabels,
} from "@/components/matching/matching-format";
import MatchScoreDisplay from "@/components/matching/MatchScoreDisplay.vue";
import InterestProfileWeights from "@/components/profile/InterestProfileWeights.vue";
import { i18n } from "@/i18n";
import type { MatchingResponse, MediaType } from "@/types/api";

const matching = ref<MatchingResponse | null>(null);
const loading = ref(true);
const errorMessage = ref("");
const selectedMediaType = ref<"ALL" | MediaType>("ALL");
const selectedTagId = ref("ALL");
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

const mediaTypeOptions = computed(() => {
  trackLocaleDependency();

  const types = new Set<MediaType>();

  for (const item of matching.value?.matches ?? []) {
    types.add(item.candidate.media.mediaType);
  }

  return Array.from(types).map((value) => ({
    value,
    label: mediaTypeLabels[value],
  }));
});
const tagOptions = computed(() =>
  Array.from(
    (matching.value?.matches ?? []).reduce((items, item) => {
      for (const tag of item.candidate.media.tags) {
        items.set(tag.id, tag.name);
      }

      return items;
    }, new Map<string, string>()),
  )
    .map(([value, label]) => ({ value, label }))
    .sort((left, right) => left.label.localeCompare(right.label)),
);
const filteredMatches = computed(() =>
  (matching.value?.matches ?? []).filter((item) => {
    if (
      selectedMediaType.value !== "ALL"
      && item.candidate.media.mediaType !== selectedMediaType.value
    ) {
      return false;
    }

    if (
      selectedTagId.value !== "ALL"
      && !item.candidate.media.tags.some((tag) => tag.id === selectedTagId.value)
    ) {
      return false;
    }

    return true;
  }),
);
const topMatches = computed(() => {
  trackLocaleDependency();
  return filteredMatches.value.slice(0, 3).map((result, index) => ({
    rank: index + 1,
    rankTone: getTopMatchRankTone(index + 1),
    result,
    summary: getGeneratedMatchExplanation(result),
  }));
});
const hasActiveFilters = computed(
  () => selectedMediaType.value !== "ALL" || selectedTagId.value !== "ALL",
);
const profileReady = computed(
  () => matching.value?.interestProfile.isReadyForMatching ?? false,
);
const profileReadinessSummary = computed(() => {
  trackLocaleDependency();
  return matching.value ? getProfileReadinessSummary(matching.value.interestProfile) : "";
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
    selectedMediaType.value = "ALL";
    selectedTagId.value = "ALL";
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

function getTopMatchRankTone(rank: number): "gold" | "silver" | "bronze" {
  if (rank === 1) {
    return "gold";
  }

  if (rank === 2) {
    return "silver";
  }

  return "bronze";
}

function getMatchCardToneClass(score: number | string | null): string {
  return `matches-view__match-card--${getScoreTone(score)}`;
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header matches-view__header">
      <div>
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
        <article
          v-if="topMatches.length > 0"
          class="page-card matches-view__highlight"
        >
          <div class="matches-view__highlight-copy">
            <p class="eyebrow">
              {{ t("matches.highlight") }}
            </p>
            <h2 class="section-title">
              {{ t("matches.topMatchesTitle") }}
            </h2>
            <p class="body-muted">
              {{ t("matches.topMatchesCopy") }}
            </p>
          </div>

          <ol class="matches-view__highlight-list">
            <li
              v-for="entry in topMatches"
              :key="entry.result.candidate.media.id"
              class="matches-view__highlight-item"
              :class="`matches-view__highlight-item--${entry.rankTone}`"
            >
              <span
                class="matches-view__highlight-rank"
                :class="`matches-view__highlight-rank--${entry.rankTone}`"
              >
                {{ entry.rank }}
              </span>

              <div class="matches-view__highlight-item-copy">
                <p class="matches-view__highlight-meta">
                  {{ mediaTypeLabels[entry.result.candidate.media.mediaType] }}
                  <span v-if="entry.result.candidate.media.releaseYear">
                    · {{ entry.result.candidate.media.releaseYear }}
                  </span>
                </p>
                <h3 class="matches-view__highlight-title">
                  {{ getDisplayText(entry.result.candidate.media.title) }}
                </h3>
                <p class="body-muted matches-view__highlight-summary">
                  {{ entry.summary }}
                </p>
              </div>

              <MatchScoreDisplay
                class="matches-view__highlight-score"
                :score="entry.result.relativeScore"
                :suppressed="matching.scoresSuppressed"
                compact
              />
            </li>
          </ol>

          <div class="matches-view__highlight-footer">
            <p class="body-muted matches-view__highlight-note">
              {{ t("matches.scoreMethodCopy") }}
            </p>
          </div>
        </article>

        <InterestProfileWeights
          class="matches-view__weights"
          :tags="matching.interestProfile.weightedTags"
          :title="t('matches.profileWeightsTitle')"
        />
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
            <h2 class="section-title">
              {{ t("matches.cardsTitle") }}
            </h2>
            <p class="body-muted">
              {{ t("matches.cardsCopy") }}
            </p>
          </div>
        </div>

        <section
          v-if="mediaTypeOptions.length > 1 || tagOptions.length > 1"
          class="page-card matches-view__filters"
        >
          <div class="matches-view__filters-copy">
            <p class="eyebrow">
              {{ t("matches.filtersEyebrow") }}
            </p>
            <p class="body-muted">
              {{
                hasActiveFilters
                  ? t("matches.filtersActiveCopy", {
                    shown: filteredMatches.length,
                    total: matching.matches.length,
                  })
                  : t("matches.filtersIdleCopy")
              }}
            </p>
          </div>

          <div class="matches-view__filter-grid">
            <label class="matches-view__filter-field">
              <span>{{ t("matches.mediaTypeFilter") }}</span>
              <select
                v-model="selectedMediaType"
                class="input"
                name="matchMediaTypeFilter"
              >
                <option value="ALL">
                  {{ t("matches.allMediaTypes") }}
                </option>
                <option
                  v-for="option in mediaTypeOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </label>

            <label class="matches-view__filter-field">
              <span>{{ t("matches.tagFilter") }}</span>
              <select
                v-model="selectedTagId"
                class="input"
                name="matchTagFilter"
              >
                <option value="ALL">
                  {{ t("matches.allTags") }}
                </option>
                <option
                  v-for="option in tagOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>
        </section>

        <AppMessage
          v-if="filteredMatches.length === 0"
          :title="t('matches.filteredEmptyTitle')"
          :description="t('matches.filteredEmptyDescription')"
          tone="info"
        />

        <article
          v-for="result in filteredMatches"
          :key="result.candidate.media.id"
          class="page-card matches-view__match-card"
          :class="getMatchCardToneClass(result.relativeScore)"
        >
          <div class="matches-view__match-header">
            <div>
              <h2 class="section-title">
                {{ getDisplayText(result.candidate.media.title) }}
              </h2>
              <p class="matches-view__match-meta">
                {{ mediaTypeLabels[result.candidate.media.mediaType] }}
                <span v-if="result.candidate.media.releaseYear">
                  · {{ result.candidate.media.releaseYear }}
                </span>
              </p>
            </div>

            <MatchScoreDisplay
              class="matches-view__match-score"
              :score="result.relativeScore"
              :suppressed="matching.scoresSuppressed"
              compact
            />
          </div>

          <div class="matches-view__match-body">
            <CandidateSummaryCard
              class="matches-view__candidate-card"
              :candidate="result.candidate"
              :title="t('matches.baseCandidate')"
              compact
              :show-expected-note="false"
            />

            <MatchExplanation
              :result="result"
              :show-heading="false"
            />
          </div>
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

.matches-view__header .page-copy {
  max-width: 40rem;
}

.matches-view__header .page-title {
  max-width: none;
}

.matches-view__hero {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: minmax(0, 1.12fr) minmax(280px, 0.88fr);
  align-items: start;
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

.matches-view__highlight-copy {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.matches-view__highlight-copy p {
  margin: 0;
}

.matches-view__highlight-note {
  margin-top: 0;
  font-size: 0.82rem;
}

.matches-view__highlight-list {
  display: grid;
  gap: 0.62rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.matches-view__highlight-item {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 0.82rem;
  grid-template-columns: auto minmax(0, 1fr) minmax(7.1rem, auto);
  align-items: center;
  min-height: 8rem;
  padding: 0.72rem 0.8rem;
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 68%, transparent);
  border-radius: calc(var(--radius-md) + 2px);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--color-surface-secondary) 76%, transparent), transparent 46%),
    color-mix(in srgb, var(--color-surface-secondary) 82%, var(--color-surface));
  box-shadow: 0 12px 28px rgba(8, 14, 28, 0.08);
}

.matches-view__highlight-item::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4.2rem;
  background: radial-gradient(circle at 42% 34%, rgba(255, 255, 255, 0.2), transparent 58%);
  opacity: 0.72;
  pointer-events: none;
}

.matches-view__highlight-item::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(
    120deg,
    rgba(255, 255, 255, 0.18),
    transparent 24%,
    transparent 62%,
    rgba(255, 255, 255, 0.06)
  );
  pointer-events: none;
}

.matches-view__highlight-item--gold {
  border-color: color-mix(in srgb, #d1a457 30%, var(--color-border));
  background:
    linear-gradient(90deg, color-mix(in srgb, #f1d8a5 40%, transparent), transparent 46%),
    color-mix(in srgb, var(--color-surface-secondary) 82%, var(--color-surface));
  box-shadow: 0 14px 28px rgba(161, 119, 39, 0.1);
}

.matches-view__highlight-item--gold::before {
  background:
    radial-gradient(circle at 44% 32%, rgba(255, 232, 172, 0.46), transparent 58%),
    linear-gradient(90deg, rgba(209, 164, 87, 0.18), transparent 92%);
}

.matches-view__highlight-item--silver {
  border-color: color-mix(in srgb, #aab5c3 28%, var(--color-border));
  background:
    linear-gradient(90deg, color-mix(in srgb, #dbe3ed 38%, transparent), transparent 46%),
    color-mix(in srgb, var(--color-surface-secondary) 82%, var(--color-surface));
  box-shadow: 0 14px 28px rgba(123, 138, 160, 0.08);
}

.matches-view__highlight-item--silver::before {
  background:
    radial-gradient(circle at 44% 32%, rgba(232, 241, 251, 0.42), transparent 58%),
    linear-gradient(90deg, rgba(170, 181, 195, 0.16), transparent 92%);
}

.matches-view__highlight-item--bronze {
  border-color: color-mix(in srgb, #b97a54 28%, var(--color-border));
  background:
    linear-gradient(90deg, color-mix(in srgb, #ebc0a8 36%, transparent), transparent 46%),
    color-mix(in srgb, var(--color-surface-secondary) 82%, var(--color-surface));
  box-shadow: 0 14px 28px rgba(156, 100, 68, 0.09);
}

.matches-view__highlight-item--bronze::before {
  background:
    radial-gradient(circle at 44% 32%, rgba(241, 194, 158, 0.42), transparent 58%),
    linear-gradient(90deg, rgba(185, 122, 84, 0.16), transparent 92%);
}

.matches-view__highlight-rank {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: start;
  min-width: 2.45rem;
  min-height: 2.45rem;
  padding: 0.2rem 0.48rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--color-surface-secondary) 84%, var(--color-surface));
  color: var(--color-text-primary);
  font-size: 0.94rem;
  font-weight: 800;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.16);
}

.matches-view__highlight-rank--gold {
  border-color: color-mix(in srgb, #d1a457 34%, var(--color-border));
  background: color-mix(in srgb, #f1d8a5 54%, var(--color-surface));
  color: color-mix(in srgb, #7a5210 86%, var(--color-text-primary));
  box-shadow:
    0 0 1.65rem rgba(209, 164, 87, 0.48),
    0 0 3.2rem rgba(209, 164, 87, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.22);
}

.matches-view__highlight-rank--silver {
  border-color: color-mix(in srgb, #aab5c3 34%, var(--color-border));
  background: color-mix(in srgb, #dbe3ed 58%, var(--color-surface));
  color: color-mix(in srgb, #526071 82%, var(--color-text-primary));
  box-shadow:
    0 0 1.5rem rgba(170, 181, 195, 0.42),
    0 0 2.8rem rgba(170, 181, 195, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.matches-view__highlight-rank--bronze {
  border-color: color-mix(in srgb, #b97a54 34%, var(--color-border));
  background: color-mix(in srgb, #ebc0a8 56%, var(--color-surface));
  color: color-mix(in srgb, #7d4729 84%, var(--color-text-primary));
  box-shadow:
    0 0 1.5rem rgba(185, 122, 84, 0.42),
    0 0 2.8rem rgba(185, 122, 84, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.matches-view__highlight-item-copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.22rem;
  align-content: start;
  min-width: 0;
}

.matches-view__highlight-meta,
.matches-view__highlight-item-copy p {
  margin: 0;
}

.matches-view__highlight-meta {
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.matches-view__highlight-title {
  margin: 0;
  font-size: 1.04rem;
  font-weight: 750;
  line-height: 1.12;
  overflow-wrap: anywhere;
}

.matches-view__highlight-summary {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.42;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.matches-view__highlight-score {
  position: relative;
  z-index: 1;
  align-self: stretch;
  justify-self: end;
  width: 100%;
}

.matches-view__highlight-footer {
  padding-top: 0.1rem;
  border-top: 1px solid color-mix(in srgb, var(--color-border) 78%, transparent);
}

.matches-view__list {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 28rem), 1fr));
  align-items: start;
}

.matches-view__list > .section-header,
.matches-view__list > .matches-view__filters,
.matches-view__list > .app-message {
  grid-column: 1 / -1;
}

.matches-view__filters {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 1.1fr);
  align-items: end;
  padding: 0.92rem 0.98rem 0.98rem;
  border-color: color-mix(in srgb, var(--color-border-strong) 72%, transparent);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 30%),
    color-mix(in srgb, var(--color-surface-secondary) 80%, var(--color-surface));
}

.matches-view__filters-copy {
  display: grid;
  gap: 0.3rem;
}

.matches-view__filters-copy p {
  margin: 0;
}

.matches-view__filter-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.matches-view__filter-field {
  display: grid;
  gap: 0.35rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.matches-view__filter-field span {
  color: var(--color-text-primary);
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.matches-view__filter-field .input {
  min-height: 2.95rem;
  border-color: color-mix(in srgb, var(--color-border-strong) 74%, transparent);
  background: color-mix(in srgb, var(--color-surface) 90%, var(--color-surface-secondary));
}

.matches-view__state-card {
  width: min(100%, 58rem);
}

.matches-view__match-card {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 0.78rem;
  height: 100%;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 26%),
    color-mix(in srgb, var(--theme-card-background) 92%, var(--color-surface));
}

.matches-view__match-card::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.09), transparent 30%);
  pointer-events: none;
}

.matches-view__match-card--success {
  border-color: color-mix(in srgb, var(--color-success) 26%, var(--color-border));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-success-soft) 30%, transparent), transparent 30%),
    color-mix(in srgb, var(--theme-card-background) 92%, var(--color-surface));
}

.matches-view__match-card--warning {
  border-color: color-mix(in srgb, var(--color-warning) 26%, var(--color-border));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-warning-soft) 28%, transparent), transparent 30%),
    color-mix(in srgb, var(--theme-card-background) 92%, var(--color-surface));
}

.matches-view__match-card--low {
  border-color: color-mix(in srgb, var(--color-error) 24%, var(--color-border));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-error-soft) 24%, transparent), transparent 30%),
    color-mix(in srgb, var(--theme-card-background) 92%, var(--color-surface));
}

.matches-view__match-header {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: stretch;
  gap: 0.9rem;
  padding: 0.82rem;
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 52%, transparent);
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 36%),
    color-mix(in srgb, var(--color-surface-secondary) 72%, var(--color-surface));
}

.matches-view__match-header > div {
  min-width: 0;
}

.matches-view__match-meta {
  margin: 0.22rem 0 0;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
}

.matches-view__match-header h2 {
  margin-top: 0;
  line-height: 1.1;
  overflow-wrap: anywhere;
}

.matches-view__match-score {
  align-self: stretch;
  justify-self: end;
  width: min(100%, 7.9rem);
}

.matches-view__match-body {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.78rem;
  grid-template-columns: minmax(0, 0.94fr) minmax(0, 1.06fr);
  align-items: start;
}

.matches-view__candidate-card {
  padding: 0.82rem;
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 56%, transparent);
  border-radius: var(--radius-md);
  box-shadow: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 34%),
    color-mix(in srgb, var(--color-surface-secondary) 84%, var(--color-surface));
}

.matches-view__candidate-card :deep(.candidate-summary-card__header .eyebrow) {
  font-size: 0.72rem;
}

.matches-view__candidate-card :deep(.candidate-summary-card__title) {
  display: none;
}

.matches-view__candidate-card :deep(.candidate-summary-card__media) {
  grid-template-columns: 5.2rem minmax(0, 1fr);
  gap: 0.75rem;
}

.matches-view__candidate-card :deep(.candidate-summary-card__copy-line) {
  font-size: 0.9rem;
}

.matches-view__candidate-card :deep(.candidate-summary-card__facts),
.matches-view__candidate-card :deep(.candidate-summary-card__tags) {
  gap: 0.45rem;
}

.matches-view__candidate-card :deep(.candidate-summary-card__fact) {
  min-height: 1.72rem;
  padding: 0.2rem 0.58rem;
  font-size: 0.78rem;
}

.matches-view__match-card :deep(.match-explanation__summary-copy) {
  font-size: 0.92rem;
}

.matches-view__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .matches-view__hero {
    grid-template-columns: 1fr;
  }

  .matches-view__filters,
  .matches-view__match-body {
    grid-template-columns: 1fr;
  }

  .matches-view__list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .matches-view__highlight {
    gap: 0.68rem;
  }

  .matches-view__filter-grid {
    grid-template-columns: 1fr;
  }

  .matches-view__highlight-item,
  .matches-view__match-header {
    grid-template-columns: 1fr;
  }

  .matches-view__highlight-score,
  .matches-view__match-score {
    justify-self: start;
    width: min(100%, 12rem);
  }
}
</style>
