<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { listCandidates } from "@/api/candidates";
import { ApiRequestError } from "@/api/client";
import { listMedia } from "@/api/media";
import { getMatches } from "@/api/matches";
import { getProfile } from "@/api/profile";
import AppMessage from "@/components/common/AppMessage.vue";
import DashboardSummaryCard from "@/components/dashboard/DashboardSummaryCard.vue";
import { getGeneratedMatchExplanation } from "@/components/matching/matching-copy";
import {
  consumptionStatusLabels,
  mediaTypeLabels,
} from "@/components/matching/matching-format";
import { i18n } from "@/i18n";
import type {
  CandidateSelectionResponse,
  InterestProfileResponse,
  MatchResultResponse,
  MatchingResponse,
  MediaResponse,
} from "@/types/api";
const { t } = i18n.global;

const media = ref<MediaResponse[] | null>(null);
const profile = ref<InterestProfileResponse | null>(null);
const candidateSelection = ref<CandidateSelectionResponse | null>(null);
const matching = ref<MatchingResponse | null>(null);
const loading = ref(true);
const loadWarnings = ref<string[]>([]);
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const consumedCount = computed(
  () =>
    media.value?.filter((item) => item.consumptionStatus === "CONSUMED")
      .length ?? 0,
);
const profileTagCount = computed(() => profile.value?.weightedTags.length ?? 0);
const candidateReadyCount = computed(
  () =>
    candidateSelection.value?.candidates.filter(
      (item) => item.isCompleteForMatching,
    ).length ?? 0,
);
const meaningfulMatchCount = computed(
  () =>
    matching.value?.matches.filter((item) => item.relativeScore !== null)
      .length ?? 0,
);
const mediaCount = computed(() => media.value?.length ?? 0);
const candidateCount = computed(
  () => candidateSelection.value?.candidates.length ?? 0,
);
const bestMatch = computed<MatchResultResponse | null>(
  () => matching.value?.matches[0] ?? null,
);
const recentMediaItems = computed(() =>
  [...(media.value ?? [])]
    .sort(
      (left, right) =>
        new Date(right.updatedAt).getTime() -
        new Date(left.updatedAt).getTime(),
    )
    .slice(0, 3),
);

const heroTitle = computed(() => {
  trackLocaleDependency();
  const match = bestMatch.value;

  if (mediaCount.value === 0 && candidateCount.value === 0) {
    return t("dashboard.heroEmpty");
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return t("dashboard.heroWarmup");
  }

  if (matching.value?.scoresSuppressed) {
    return t("dashboard.heroSuppressed");
  }

  if (match?.relativeScore != null) {
    return t("dashboard.heroLeading", {
      title: match.candidate.media.title,
    });
  }

  return t("dashboard.heroDefault");
});

const heroCopy = computed(() => {
  trackLocaleDependency();
  const match = bestMatch.value;

  if (mediaCount.value === 0 && candidateCount.value === 0) {
    return t("dashboard.copyEmpty");
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return t("dashboard.copyWarmup");
  }

  if (matching.value?.scoresSuppressed) {
    return t("dashboard.copySuppressed", {
      message: "",
    }).trim();
  }

  if (match?.relativeScore != null) {
    return t("dashboard.copyLeading");
  }

  return t("dashboard.copyDefault");
});

const nextAction = computed(() => {
  trackLocaleDependency();

  if (mediaCount.value === 0) {
    return {
      title: t("dashboard.nextCreateTitle"),
      copy: t("dashboard.nextCreateCopy"),
      to: { name: "media-create" as const },
      label: t("common.actions.createMedia"),
    };
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return {
      title: t("dashboard.nextProfileTitle"),
      copy: t("dashboard.nextProfileCopy"),
      to: { name: "profile" as const },
      label: t("common.actions.openProfile"),
    };
  }

  if (candidateCount.value === 0) {
    return {
      title: t("dashboard.nextCandidatesTitle"),
      copy: t("dashboard.nextCandidatesCopy"),
      to: { name: "media-create" as const },
      label: t("common.actions.createCandidate"),
    };
  }

  return {
    title: t("dashboard.nextMatchesTitle"),
    copy: t("dashboard.nextMatchesCopy"),
    to: { name: "matches" as const },
    label: t("common.actions.openMatches"),
  };
});

const focusCard = computed(() => {
  trackLocaleDependency();

  if (bestMatch.value) {
    return {
      title: bestMatch.value.candidate.media.title,
      copy: getGeneratedMatchExplanation(bestMatch.value),
      to: { name: "matches" as const },
      label: t("common.actions.readMatch"),
    };
  }

  if (candidateCount.value > 0) {
    return {
      title: t("dashboard.focusWaitingTitle"),
      copy: t("dashboard.focusWaitingCopy"),
      to: { name: "candidates" as const },
      label: t("common.actions.openCandidates"),
    };
  }

  return {
    title: t("dashboard.focusEmptyTitle"),
    copy: t("dashboard.focusEmptyCopy"),
    to: { name: "candidates" as const },
    label: t("common.actions.openCandidates"),
  };
});

onMounted(async () => {
  await loadDashboard();
});

async function loadDashboard() {
  loading.value = true;
  loadWarnings.value = [];

  const results = await Promise.allSettled([
    listMedia(),
    getProfile(),
    listCandidates(),
    getMatches(),
  ]);

  const [mediaResult, profileResult, candidatesResult, matchesResult] = results;

  if (mediaResult.status === "fulfilled") {
    media.value = mediaResult.value;
  } else {
    media.value = null;
    loadWarnings.value.push(
      `${t("navigation.media")}: ${toUserMessage(mediaResult.reason, t("dashboard.loadMediaError"))}`,
    );
  }

  if (profileResult.status === "fulfilled") {
    profile.value = profileResult.value;
  } else {
    profile.value = null;
    loadWarnings.value.push(
      `${t("navigation.profile")}: ${toUserMessage(profileResult.reason, t("dashboard.loadProfileError"))}`,
    );
  }

  if (candidatesResult.status === "fulfilled") {
    candidateSelection.value = candidatesResult.value;
  } else {
    candidateSelection.value = null;
    loadWarnings.value.push(
      `${t("navigation.candidates")}: ${toUserMessage(candidatesResult.reason, t("dashboard.loadCandidatesError"))}`,
    );
  }

  if (matchesResult.status === "fulfilled") {
    matching.value = matchesResult.value;
  } else {
    matching.value = null;
    loadWarnings.value.push(
      `${t("navigation.matches")}: ${toUserMessage(matchesResult.reason, t("dashboard.loadMatchesError"))}`,
    );
  }

  loading.value = false;
}

function toUserMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return fallback;
}

const mediaSummaryDescription = computed(() => {
  trackLocaleDependency();

  if (!media.value) {
    return t("dashboard.summaryMediaFallback");
  }

  return t("dashboard.summaryMediaDescription", {
    consumed: consumedCount.value,
    other: media.value.length - consumedCount.value,
  });
});

const profileSummaryDescription = computed(() => {
  trackLocaleDependency();

  if (!profile.value) {
    return t("dashboard.summaryProfileFallback");
  }

  return profile.value.isReadyForMatching
    ? t("dashboard.summaryProfileReadyDescription", { count: profileTagCount.value })
    : t("dashboard.summaryProfileBuildingDescription", { count: profileTagCount.value });
});

const candidatesSummaryDescription = computed(() => {
  trackLocaleDependency();

  if (!candidateSelection.value) {
    return t("dashboard.summaryCandidatesFallback");
  }

  return t("dashboard.summaryCandidatesDescription", {
    ready: candidateReadyCount.value,
    missing: candidateSelection.value.candidates.length - candidateReadyCount.value,
  });
});

const matchesSummaryDescription = computed(() => {
  trackLocaleDependency();

  if (!matching.value) {
    return t("dashboard.summaryMatchesFallback");
  }

  return matching.value.scoresSuppressed
    ? t("dashboard.summaryMatchesWaitingDescription", { count: meaningfulMatchCount.value })
    : t("dashboard.summaryMatchesReadyDescription", { count: meaningfulMatchCount.value });
});
</script>

<template>
  <section class="dashboard page-stack">
    <header class="dashboard__hero page-card">
      <div class="dashboard__hero-copy">
        <p class="eyebrow">
          {{ t("dashboard.eyebrow") }}
        </p>
        <h1 class="page-title">
          {{ heroTitle }}
        </h1>
        <p class="page-copy">
          {{ heroCopy }}
        </p>

        <div class="dashboard__hero-badges">
          <span class="badge"> {{ t("dashboard.statsMedia", { count: mediaCount }) }} </span>
          <span
            class="badge"
            :class="
              profile?.isReadyForMatching ? 'badge--success' : 'badge--warning'
            "
          >
            {{
              profile?.isReadyForMatching ? t("dashboard.statsProfileReady") : t("dashboard.statsProfileBuilding")
            }}
          </span>
          <span
            class="badge"
            :class="meaningfulMatchCount > 0 ? 'badge--accent' : ''"
          >
            {{ t("dashboard.statsMeaningfulMatches", { count: meaningfulMatchCount }) }}
          </span>
        </div>
      </div>

      <aside class="dashboard__hero-side">
        <article class="dashboard__hero-panel">
          <p class="eyebrow">
            {{ t("dashboard.nextStep") }}
          </p>
          <h2 class="section-title">
            {{ nextAction.title }}
          </h2>
          <p class="body-muted">
            {{ nextAction.copy }}
          </p>

          <div class="dashboard__hero-panel-actions">
            <RouterLink
              :to="nextAction.to"
              class="button button--primary"
            >
              {{ nextAction.label }}
            </RouterLink>
            <RouterLink
              :to="focusCard.to"
              class="button button--secondary"
            >
              {{ focusCard.label }}
            </RouterLink>
          </div>
        </article>

        <div class="dashboard__quick-actions">
          <RouterLink
            :to="{ name: 'media-list' }"
            class="button button--secondary"
          >
            {{ t("common.actions.openMediaLibrary") }}
          </RouterLink>
          <RouterLink
            :to="{ name: 'profile' }"
            class="button button--secondary"
          >
            {{ t("navigation.profile") }}
          </RouterLink>
          <RouterLink
            :to="{ name: 'swipe' }"
            class="button button--secondary"
          >
            {{ t("navigation.swipe") }}
          </RouterLink>
        </div>
      </aside>
    </header>

    <AppMessage
      v-if="loading"
      :title="t('dashboard.loadingTitle')"
      :description="t('dashboard.loadingDescription')"
      tone="info"
    />

    <template v-else>
      <AppMessage
        v-if="loadWarnings.length > 0"
        :title="t('dashboard.partialLoadTitle')"
        :description="loadWarnings.join('\n')"
        tone="warning"
      >
        <div class="dashboard__message-actions">
          <button
            class="button button--secondary"
            type="button"
            @click="loadDashboard"
          >
            {{ t("common.actions.retry") }}
          </button>
        </div>
      </AppMessage>

      <section class="page-section">
        <div class="section-header">
          <div class="section-header__copy">
            <p class="eyebrow">
              {{ t("dashboard.overviewEyebrow") }}
            </p>
            <h2 class="section-title">
              {{ t("dashboard.overviewTitle") }}
            </h2>
            <p class="body-muted">
              {{ t("dashboard.overviewCopy") }}
            </p>
          </div>
        </div>

        <div class="dashboard__grid">
          <DashboardSummaryCard
            :eyebrow="t('navigation.media')"
            :value="String(media?.length ?? '–')"
            :title="t('dashboard.summaryMediaTitle')"
            :description="mediaSummaryDescription"
            :link-to="{ name: 'media-list' }"
            :link-label="t('common.actions.openMediaLibrary')"
            tone="info"
          />

          <DashboardSummaryCard
            :eyebrow="t('navigation.profile')"
            :value="
              profile
                ? `${profile.profileRelevantMediaCount}/${profile.requiredProfileRelevantMediaCount}`
                : '–'
            "
            :title="t('dashboard.summaryProfileTitle')"
            :description="profileSummaryDescription"
            :link-to="{ name: 'profile' }"
            :link-label="t('common.actions.openProfile')"
            :tone="profile?.isReadyForMatching ? 'success' : 'warning'"
          />

          <DashboardSummaryCard
            :eyebrow="t('navigation.candidates')"
            :value="String(candidateSelection?.candidates.length ?? '–')"
            :title="t('dashboard.summaryCandidatesTitle')"
            :description="candidatesSummaryDescription"
            :link-to="{ name: 'candidates' }"
            :link-label="t('common.actions.openCandidates')"
            tone="default"
          />

          <DashboardSummaryCard
            :eyebrow="t('navigation.matches')"
            :value="String(matching?.matches.length ?? '–')"
            :title="t('dashboard.summaryMatchesTitle')"
            :description="matchesSummaryDescription"
            :link-to="{ name: 'matches' }"
            :link-label="t('common.actions.readMatch')"
            :tone="matching?.scoresSuppressed ? 'warning' : 'success'"
          />
        </div>
      </section>

      <section class="dashboard__detail-grid">
        <article class="page-card dashboard__detail-card">
          <p class="eyebrow">
            {{ t("dashboard.focusEyebrow") }}
          </p>
          <h2 class="section-title">
            {{ focusCard.title }}
          </h2>
          <p class="body-muted">
            {{ focusCard.copy }}
          </p>
          <RouterLink
            :to="focusCard.to"
            class="button button--secondary"
          >
            {{ focusCard.label }}
          </RouterLink>
        </article>

        <article class="page-card dashboard__detail-card">
          <div class="dashboard__detail-heading">
            <p class="eyebrow">
              {{ t("dashboard.recentEyebrow") }}
            </p>
            <h2 class="section-title">
              {{
                recentMediaItems.length > 0
                  ? t("dashboard.recentTitle")
                  : t("dashboard.recentEmptyTitle")
              }}
            </h2>
            <p class="body-muted">
              {{
                recentMediaItems.length > 0
                  ? t("dashboard.recentCopy")
                  : t("dashboard.recentEmptyCopy")
              }}
            </p>
          </div>

          <ul
            v-if="recentMediaItems.length > 0"
            class="dashboard__recent-list"
          >
            <li
              v-for="item in recentMediaItems"
              :key="item.id"
              class="dashboard__recent-item"
            >
              <div>
                <p class="dashboard__recent-title">
                  {{ item.title }}
                </p>
                <p class="dashboard__recent-meta">
                  {{ mediaTypeLabels[item.mediaType] }} ·
                  {{ consumptionStatusLabels[item.consumptionStatus]
                  }}<span v-if="item.releaseYear">
                    · {{ item.releaseYear }}</span>
                </p>
              </div>
              <span class="badge"> {{ t("dashboard.recentTagCount", { count: item.tags.length }) }} </span>
            </li>
          </ul>

          <RouterLink
            :to="{ name: 'media-list' }"
            class="button button--secondary"
          >
            {{ t("common.actions.openMediaLibrary") }}
          </RouterLink>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.dashboard__hero {
  display: grid;
  gap: 0.95rem;
  grid-template-columns: minmax(0, 1.45fr) minmax(260px, 0.9fr);
  padding: clamp(1.08rem, 2.3vw, 1.4rem);
  background: radial-gradient(
      circle at top right,
      color-mix(in srgb, var(--color-accent-soft) 58%, transparent),
      transparent 31%
    ),
    linear-gradient(
      180deg,
      color-mix(
        in srgb,
        var(--color-surface-secondary) 72%,
        var(--color-surface)
      ),
      var(--color-surface)
    );
}

.dashboard__hero-copy {
  display: grid;
  gap: 0.55rem;
}

.dashboard__hero-copy .page-copy {
  margin: 0;
}

.dashboard__hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.dashboard__hero-side {
  display: grid;
  gap: 0.75rem;
  align-content: start;
}

.dashboard__hero-panel,
.dashboard__detail-card {
  display: grid;
  gap: 0.72rem;
}

.dashboard__hero-panel {
  padding: 0.92rem;
  border: 1px solid var(--color-border);
  border-radius: calc(var(--radius-lg) - 6px);
  background: color-mix(
    in srgb,
    var(--color-surface-secondary) 76%,
    var(--color-surface)
  );
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.dashboard__grid,
.dashboard__detail-grid {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.dashboard__detail-grid {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.dashboard__detail-card {
  padding: 1rem;
}

.dashboard__detail-heading {
  display: grid;
  gap: 0.3rem;
}

.dashboard__recent-list {
  display: grid;
  gap: 0.6rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.dashboard__recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.72rem 0.82rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(
    in srgb,
    var(--color-surface-secondary) 74%,
    transparent
  );
}

.dashboard__hero-panel p,
.dashboard__detail-card p {
  margin: 0;
}

.dashboard__hero-panel-actions,
.dashboard__quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.dashboard__quick-actions > * {
  flex: 1 1 8rem;
}

.dashboard__recent-title,
.dashboard__recent-meta {
  margin: 0;
}

.dashboard__recent-title {
  font-weight: 700;
}

.dashboard__recent-meta {
  margin-top: 0.2rem;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.dashboard__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .dashboard__hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard__quick-actions > * {
    flex: 1 1 10rem;
  }
}

@media (max-width: 560px) {
  .dashboard__hero-panel-actions > *,
  .dashboard__quick-actions > * {
    width: 100%;
  }

  .dashboard__recent-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
