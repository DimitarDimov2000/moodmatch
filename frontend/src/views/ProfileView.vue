<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { getProfile } from "@/api/profile";
import AppMessage from "@/components/common/AppMessage.vue";
import { getProfileReadinessSummary } from "@/components/matching/matching-copy";
import ProfileContributionCard from "@/components/profile/ProfileContributionCard.vue";
import InterestProfileWeights from "@/components/profile/InterestProfileWeights.vue";
import { i18n } from "@/i18n";
import type { InterestProfileResponse } from "@/types/api";

const profile = ref<InterestProfileResponse | null>(null);
const loading = ref(true);
const errorMessage = ref("");
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const strongSignalCount = computed(
  () => profile.value?.weightedTags.length ?? 0,
);
const contributionCount = computed(
  () => profile.value?.contributingMedia.length ?? 0,
);
const readinessSummary = computed(() => {
  trackLocaleDependency();
  return profile.value ? getProfileReadinessSummary(profile.value) : "";
});

onMounted(async () => {
  await loadProfile();
});

async function loadProfile() {
  loading.value = true;
  errorMessage.value = "";

  try {
    profile.value = await getProfile();
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

  return t("profile.errorTitle");
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header profile-view__header">
      <div>
        <h1 class="page-title">
          {{ t("profile.title") }}
        </h1>
        <p class="page-copy">
          {{ t("profile.intro") }}
        </p>
      </div>
    </header>

    <AppMessage
      v-if="loading"
      :title="t('profile.loadingTitle')"
      :description="t('profile.loadingDescription')"
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      :title="t('profile.errorTitle')"
      :description="errorMessage"
      tone="error"
    >
      <div class="profile-view__message-actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadProfile"
        >
          {{ t("common.actions.retry") }}
        </button>
      </div>
    </AppMessage>

    <template v-else-if="profile">
      <section class="overview-stats">
        <article
          class="page-card overview-stat-card"
          :class="profile.isReadyForMatching ? 'overview-stat-card--success' : 'overview-stat-card--warning'"
        >
          <p class="eyebrow">
            {{ t("profile.readiness") }}
          </p>
          <p class="overview-stat-card__value">
            {{ profile.isReadyForMatching ? t("profile.yes") : t("profile.notYet") }}
          </p>
          <p class="overview-stat-card__copy">
            {{ readinessSummary }}
          </p>
        </article>

        <article class="page-card overview-stat-card overview-stat-card--info">
          <p class="eyebrow">
            {{ t("profile.relevantMedia") }}
          </p>
          <p class="overview-stat-card__value">
            {{ profile.profileRelevantMediaCount }} /
            {{ profile.requiredProfileRelevantMediaCount }}
          </p>
          <p class="overview-stat-card__copy">
            {{ t("profile.requiredAtLeast", { count: profile.requiredProfileRelevantMediaCount }) }}
          </p>
        </article>

        <article class="page-card overview-stat-card">
          <p class="eyebrow">
            {{ t("profile.strongSignals") }}
          </p>
          <p class="overview-stat-card__value">
            {{ strongSignalCount }}
          </p>
          <p class="overview-stat-card__copy">
            {{ t("profile.strongestSignalsCopy") }}
          </p>
        </article>
        <article class="page-card overview-stat-card overview-stat-card--info">
          <p class="eyebrow">
            {{ t("profile.signalSource") }}
          </p>
          <p class="overview-stat-card__value">
            {{ contributionCount }}
          </p>
          <p class="overview-stat-card__copy">
            {{ t("profile.signalSourceDetails") }}
          </p>
        </article>
      </section>

      <AppMessage
        v-if="!profile.isReadyForMatching"
        :title="t('profile.notEnoughTitle')"
        :description="readinessSummary"
        tone="warning"
      >
        <div class="state-actions">
          <RouterLink
            :to="{ name: 'media-list' }"
            class="button button--secondary"
          >
            {{ t("common.actions.openMediaLibrary") }}
          </RouterLink>
        </div>
      </AppMessage>

      <section class="profile-view__section-break">
        <div class="profile-view__section-break-copy">
          <h2 class="section-title">
            {{ t("profile.whatShapesRecommendations") }}
          </h2>
          <p class="body-muted">
            {{ t("profile.recommendationFactors") }}
          </p>
        </div>
      </section>

      <section class="profile-view__layout">
        <InterestProfileWeights
          class="profile-view__weights"
          :tags="profile.weightedTags"
          :title="t('profile.profileSignals')"
        />

        <section class="profile-view__media">
          <AppMessage
            v-if="profile.contributingMedia.length === 0"
            :title="t('profile.noSourcesTitle')"
            :description="t('profile.noSourcesDescription')"
          />

          <div
            v-else
            class="profile-view__contributions"
          >
            <ProfileContributionCard
              v-for="contribution in profile.contributingMedia"
              :key="contribution.media.id"
              :contribution="contribution"
            />
          </div>
        </section>
      </section>
    </template>
  </section>
</template>

<style scoped>
.profile-view__header .page-title {
  max-width: none;
}

.profile-view__header .page-copy {
  max-width: 40rem;
}

.profile-view__layout {
  display: grid;
  gap: 1.2rem;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.1fr);
  align-items: start;
}

.profile-view__weights,
.profile-view__media,
.profile-view__contributions,
.profile-view__section-break-copy {
  display: grid;
  gap: 0.85rem;
  min-width: 0;
}

.profile-view__section-break {
  display: grid;
  gap: 0.5rem;
  margin-top: 0.42rem;
  padding-top: 0.86rem;
}

.profile-view__section-break::before {
  content: "";
  width: min(100%, 8rem);
  height: 1px;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, var(--color-accent) 58%, var(--color-border)),
    color-mix(in srgb, var(--color-border) 24%, transparent)
  );
}

.profile-view__section-break-copy {
  gap: 0.34rem;
  max-width: 42rem;
}

.profile-view__section-break-copy .section-title {
  font-size: clamp(1.02rem, 1.65vw, 1.18rem);
}

.profile-view__section-break-copy .body-muted {
  max-width: 58ch;
  line-height: 1.45;
}

.profile-view__section-break-copy p,
.profile-view__section-break-copy h2 {
  margin: 0;
}

.profile-view__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .profile-view__layout {
    grid-template-columns: 1fr;
  }
}
</style>
