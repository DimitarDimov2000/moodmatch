<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { getProfile } from "@/api/profile";
import AppMessage from "@/components/common/AppMessage.vue";
import { getProfileReadinessSummary } from "@/components/matching/matching-copy";
import ProfileContributionCard from "@/components/profile/ProfileContributionCard.vue";
import InterestProfileWeights from "@/components/profile/InterestProfileWeights.vue";
import TagChip from "@/components/tags/TagChip.vue";
import { i18n } from "@/i18n";
import type { InterestProfileResponse } from "@/types/api";

const profile = ref<InterestProfileResponse | null>(null);
const loading = ref(true);
const errorMessage = ref("");
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const strongestTags = computed(
  () => profile.value?.weightedTags.slice(0, 6) ?? [],
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
    <header class="page-header">
      <div>
        <p class="eyebrow">
          {{ t("profile.eyebrow") }}
        </p>
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
        <article class="page-card overview-stat-card">
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

        <article class="page-card overview-stat-card">
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
            {{ strongestTags.length }}
          </p>
          <p class="overview-stat-card__copy">
            {{ t("profile.strongestSignalsCopy") }}
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

      <section class="page-card profile-view__summary">
        <div class="section-header">
          <div class="section-header__copy">
            <p class="eyebrow">
              {{ t("profile.summary") }}
            </p>
            <h2 class="section-title">
              {{ t("profile.strongestSignals") }}
            </h2>
            <p class="body-muted">
              {{ readinessSummary }}
            </p>
          </div>
        </div>

        <div class="profile-view__summary-grid">
          <div class="profile-view__spotlight">
            <p class="profile-view__spotlight-label">
              {{ t("profile.strongestSignals") }}
            </p>
            <div
              v-if="strongestTags.length > 0"
              class="profile-view__spotlight-tags"
            >
              <TagChip
                v-for="item in strongestTags"
                :key="item.tag.id"
                :tag="item.tag"
              />
            </div>
            <p
              v-else
              class="body-muted"
            >
              {{ t("profile.noSignalsYet") }}
            </p>
          </div>

          <div class="profile-view__summary-notes">
            <div class="profile-view__summary-note">
              <p class="profile-view__spotlight-label">
                {{ t("profile.whatShapesRecommendations") }}
              </p>
              <p class="body-muted">
                {{ t("profile.recommendationFactors") }}
              </p>
            </div>

            <div class="profile-view__summary-note">
              <p class="profile-view__spotlight-label">
                {{ t("profile.signalSource") }}
              </p>
              <p class="body-muted">
                {{ t("profile.contributingMedia", { count: contributionCount }) }}
              </p>
            </div>
          </div>
        </div>
      </section>

      <section class="profile-view__layout">
        <InterestProfileWeights
          class="profile-view__weights"
          :tags="profile.weightedTags"
          :title="t('profile.strongestSignals')"
        />

        <section class="profile-view__media">
          <div class="profile-view__section-header">
            <h2 class="section-title">
              {{ t("profile.whatShapesRecommendations") }}
            </h2>
            <p class="body-muted">
              {{ t("profile.signalSourceDetails") }}
            </p>
          </div>

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
.profile-view__layout {
  display: grid;
  gap: 0.95rem;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.35fr);
}

.profile-view__summary {
  display: grid;
  gap: 0.9rem;
  padding: clamp(0.95rem, 2.3vw, 1.1rem);
}

.profile-view__summary-grid {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.8fr);
}

.profile-view__spotlight {
  display: grid;
  gap: 0.55rem;
}

.profile-view__spotlight-label {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.profile-view__spotlight-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.profile-view__summary-notes {
  display: grid;
  gap: 0.65rem;
}

.profile-view__summary-note {
  display: grid;
  gap: 0.35rem;
  padding: 0.78rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(
    in srgb,
    var(--color-surface-secondary) 78%,
    var(--color-surface)
  );
}

.profile-view__summary-note p {
  margin: 0;
}

.profile-view__media,
.profile-view__contributions {
  display: grid;
  gap: 0.8rem;
}

.profile-view__section-header p {
  margin: 0.25rem 0 0;
}

.profile-view__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .profile-view__layout {
    grid-template-columns: 1fr;
  }

  .profile-view__summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
