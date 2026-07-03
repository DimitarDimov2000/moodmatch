<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { listCandidates } from "@/api/candidates";
import AppMessage from "@/components/common/AppMessage.vue";
import CandidateSummaryCard from "@/components/matching/CandidateSummaryCard.vue";
import { i18n } from "@/i18n";
import type { CandidateMediaResponse } from "@/types/api";

const candidates = ref<CandidateMediaResponse[]>([]);
const loading = ref(true);
const errorMessage = ref("");
const { t } = i18n.global;

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

  return t("candidates.errorTitle");
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          {{ t("candidates.eyebrow") }}
        </p>
        <h1 class="page-title">
          {{ t("candidates.title") }}
        </h1>
        <p class="page-copy">
          {{ t("candidates.intro") }}
        </p>
      </div>

      <div class="page-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--primary"
        >
          {{ t("common.actions.createCandidate") }}
        </RouterLink>
      </div>
    </header>

    <section
      v-if="!loading && !errorMessage"
      class="overview-stats"
    >
      <article class="page-card overview-stat-card">
        <p class="eyebrow">
          {{ t("candidates.total") }}
        </p>
        <p class="overview-stat-card__value">
          {{ candidates.length }}
        </p>
        <p class="overview-stat-card__copy">
          {{ t("candidates.wantToConsume") }}
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--success">
        <p class="eyebrow">
          {{ t("candidates.matchingReady") }}
        </p>
        <p class="overview-stat-card__value">
          {{ completeCount }}
        </p>
        <p class="overview-stat-card__copy">
          {{ t("candidates.withExpectedTags") }}
        </p>
      </article>

      <article class="page-card overview-stat-card overview-stat-card--warning">
        <p class="eyebrow">
          {{ t("candidates.needsCare") }}
        </p>
        <p class="overview-stat-card__value">
          {{ incompleteCount }}
        </p>
        <p class="overview-stat-card__copy">
          {{ t("candidates.missingMatchingData") }}
        </p>
      </article>
    </section>

    <AppMessage
      v-if="loading"
      class="candidates-view__state-card"
      :title="t('candidates.loadingTitle')"
      :description="t('candidates.loadingDescription')"
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      class="candidates-view__state-card"
      :title="t('candidates.errorTitle')"
      :description="errorMessage"
      tone="error"
    >
      <div class="candidates-view__message-actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadCandidates"
        >
          {{ t("common.actions.retry") }}
        </button>
      </div>
    </AppMessage>

    <AppMessage
      v-else-if="candidates.length === 0"
      class="candidates-view__state-card"
      :title="t('candidates.emptyTitle')"
      :description="t('candidates.emptyDescription')"
    >
      <div class="state-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--primary"
        >
          {{ t("candidates.emptyAction") }}
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
            {{ t("candidates.compareList") }}
          </p>
          <h2 class="section-title">
            {{ t("candidates.beforeDecision") }}
          </h2>
          <p class="body-muted">
            {{ t("candidates.sectionCopy") }}
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
