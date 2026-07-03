<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { listCandidates } from '@/api/candidates';
import { getMatches } from '@/api/matches';
import AppMessage from '@/components/common/AppMessage.vue';
import { getProfileReadinessSummary } from '@/components/matching/matching-copy';
import SwipeCandidateCard from '@/components/swipe/SwipeCandidateCard.vue';
import SwipeDecisionControls from '@/components/swipe/SwipeDecisionControls.vue';
import SwipeProgress from '@/components/swipe/SwipeProgress.vue';
import { i18n } from '@/i18n';
import type {
  MatchResultResponse,
  MatchingResponse,
} from '@/types/api';
import type {
  SwipeDecisionAction,
  SwipeQueueItem,
  SwipeQueueStats,
} from '@/types/swipe';

interface SwipeCandidateCardHandle {
  playDecisionAnimation: (action: SwipeDecisionAction) => Promise<void>;
  resetGesturePosition: () => void;
}
const { t } = i18n.global;

const activeCardRef = ref<SwipeCandidateCardHandle | null>(null);
const queue = ref<SwipeQueueItem[]>([]);
const totalCount = ref(0);
const loading = ref(true);
const fatalErrorMessage = ref('');
const matchWarningMessage = ref('');
const actionErrorMessage = ref('');
const decisionPending = ref(false);
const detailsExpanded = ref(false);
const matching = ref<MatchingResponse | null>(null);
const liveMessage = ref('');
const stats = reactive<SwipeQueueStats>({
  liked: 0,
  skipped: 0,
});
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const currentItem = computed(() => queue.value[0] ?? null);
const nextItem = computed(() => queue.value[1] ?? null);
const remainingCount = computed(() => queue.value.length);
const hasQueue = computed(() => totalCount.value > 0);
const interactionLocked = computed(() => decisionPending.value);
const matchInsightsAvailable = computed(() => matching.value !== null);
const profileReady = computed(() => matching.value?.interestProfile.isReadyForMatching ?? null);
const scoresSuppressed = computed(() => matching.value?.scoresSuppressed ?? false);
const profileStatusMessage = computed(
  () => {
    trackLocaleDependency();

    return matching.value?.interestProfile
      ? getProfileReadinessSummary(matching.value.interestProfile)
      : t('swipe.fallbackProfileStatus');
  },
);
const showDoneState = computed(
  () => !loading.value && !fatalErrorMessage.value && hasQueue.value && remainingCount.value === 0,
);
const showProfileWarmupBanner = computed(
  () => !loading.value && !fatalErrorMessage.value && profileReady.value === false && hasQueue.value,
);
const showProfileWarmupEmptyState = computed(
  () => !loading.value && !fatalErrorMessage.value && !hasQueue.value && profileReady.value === false,
);
const showNoRecommendationsState = computed(
  () =>
    !loading.value &&
    !fatalErrorMessage.value &&
    !showDoneState.value &&
    !hasQueue.value &&
    profileReady.value !== false,
);

onMounted(async () => {
  window.addEventListener('keydown', handleWindowKeydown);
  await loadQueue();
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleWindowKeydown);
});

async function loadQueue() {
  loading.value = true;
  fatalErrorMessage.value = '';
  matchWarningMessage.value = '';
  actionErrorMessage.value = '';
  decisionPending.value = false;
  detailsExpanded.value = false;
  matching.value = null;
  liveMessage.value = '';
  stats.liked = 0;
  stats.skipped = 0;

  const [candidatesResult, matchesResult] = await Promise.allSettled([
    listCandidates(),
    getMatches(),
  ]);

  if (candidatesResult.status === 'rejected') {
    fatalErrorMessage.value = toCandidatesErrorMessage(candidatesResult.reason);
    queue.value = [];
    totalCount.value = 0;
    loading.value = false;
    return;
  }

  const matchesById = new Map<string, MatchResultResponse>();
  if (matchesResult.status === 'fulfilled') {
    matching.value = matchesResult.value;
    for (const match of matchesResult.value.matches) {
      matchesById.set(match.candidate.media.id, match);
    }
  } else {
    matchWarningMessage.value = toMatchesErrorMessage(matchesResult.reason);
  }

  queue.value = candidatesResult.value.candidates.map((candidate) => ({
    candidate,
    match: matchesById.get(candidate.media.id) ?? null,
  }));
  totalCount.value = queue.value.length;
  loading.value = false;

  if (queue.value.length > 0) {
    liveMessage.value = t('swipe.liveQueueActive', {
      title: queue.value[0].candidate.media.title,
    });
  }
}

async function handleLike() {
  if (!currentItem.value || interactionLocked.value) {
    return;
  }

  const title = currentItem.value.candidate.media.title;
  actionErrorMessage.value = '';
  decisionPending.value = true;

  await activeCardRef.value?.playDecisionAnimation('like');

  stats.liked += 1;
  const nextTitle = removeCurrentItem();
  liveMessage.value = nextTitle
    ? t('swipe.liveLikedNext', { title, next: nextTitle })
    : t('swipe.liveLikedDone', { title });
  decisionPending.value = false;
}

async function handleSkip() {
  if (!currentItem.value || interactionLocked.value) {
    return;
  }

  const title = currentItem.value.candidate.media.title;
  actionErrorMessage.value = '';
  decisionPending.value = true;

  await activeCardRef.value?.playDecisionAnimation('skip');

  stats.skipped += 1;
  const nextTitle = removeCurrentItem();
  liveMessage.value = nextTitle
    ? t('swipe.liveSkippedNext', { title, next: nextTitle })
    : t('swipe.liveSkippedDone', { title });
  decisionPending.value = false;
}

function handleDetailsToggle() {
  if (!currentItem.value || interactionLocked.value) {
    return;
  }

  actionErrorMessage.value = '';
  detailsExpanded.value = !detailsExpanded.value;
  liveMessage.value = detailsExpanded.value
    ? t('swipe.liveDetailsOpened', { title: currentItem.value.candidate.media.title })
    : t('swipe.liveDetailsClosed', { title: currentItem.value.candidate.media.title });
}

function handleDetailsLoadError(message: string) {
  actionErrorMessage.value = message;
  liveMessage.value = t('swipe.liveDetailsError', {
    title: currentItem.value?.candidate.media.title ?? t('routes.mediaDetail.title'),
  });
}

function removeCurrentItem(): string | null {
  detailsExpanded.value = false;
  queue.value = queue.value.slice(1);
  return queue.value[0]?.candidate.media.title ?? null;
}

function handleWindowKeydown(event: KeyboardEvent) {
  if (
    event.defaultPrevented ||
    interactionLocked.value ||
    !currentItem.value ||
    event.altKey ||
    event.ctrlKey ||
    event.metaKey
  ) {
    return;
  }

  if (isInteractiveTarget(event.target)) {
    return;
  }

  if (event.key === 'ArrowRight') {
    event.preventDefault();
    void handleLike();
    return;
  }

  if (event.key === 'ArrowLeft') {
    event.preventDefault();
    void handleSkip();
    return;
  }

  if (event.key === 'Enter') {
    event.preventDefault();
    handleDetailsToggle();
  }
}

function handleGestureDecision(action: SwipeDecisionAction) {
  if (action === 'like') {
    void handleLike();
    return;
  }

  void handleSkip();
}

function isInteractiveTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }

  return Boolean(target.closest('button, a, input, select, textarea, [contenteditable="true"]'));
}

function toCandidatesErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return t('swipe.fallbackError');
}

function toMatchesErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return `${error.message} ${t('swipe.matchesWarningFallback')}`;
  }

  return t('swipe.matchesWarningFallback');
}
</script>

<template>
  <section class="swipe-view">
    <header class="swipe-view__header">
      <div class="swipe-view__header-copy">
        <p class="swipe-view__eyebrow">
          {{ t('swipe.eyebrow') }}
        </p>
        <h1 class="swipe-view__title">
          {{ t('swipe.title') }}
        </h1>
        <p class="swipe-view__copy">
          {{ t('swipe.intro') }}
        </p>
      </div>

      <div class="swipe-view__actions">
        <RouterLink
          :to="{ name: 'candidates' }"
          class="button swipe-view__action-button"
        >
          {{ t('swipe.openCandidates') }}
        </RouterLink>
        <RouterLink
          :to="{ name: 'external-search' }"
          class="button swipe-view__action-button swipe-view__action-button--ghost"
        >
          {{ t('swipe.import') }}
        </RouterLink>
      </div>
    </header>

    <p
      class="swipe-view__sr-only"
      aria-live="polite"
    >
      {{ liveMessage }}
    </p>

    <AppMessage
      v-if="loading"
      :title="t('swipe.loadingTitle')"
      :description="t('swipe.loadingDescription')"
      tone="info"
    />

    <AppMessage
      v-else-if="fatalErrorMessage"
      :title="t('swipe.errorTitle')"
      :description="fatalErrorMessage"
      tone="error"
    >
      <div class="swipe-view__message-actions">
        <button
          class="button swipe-view__message-button"
          type="button"
          @click="loadQueue"
        >
          {{ t('common.actions.retry') }}
        </button>
      </div>
    </AppMessage>

    <template v-else>
      <AppMessage
        v-if="matchWarningMessage"
        :title="t('swipe.matchesWarningTitle')"
        :description="matchWarningMessage"
        tone="warning"
      />

      <AppMessage
        v-if="actionErrorMessage"
        :title="t('swipe.actionErrorTitle')"
        :description="actionErrorMessage"
        tone="error"
      />

      <AppMessage
        v-if="showProfileWarmupBanner"
        :title="t('swipe.warmupTitle')"
        :description="profileStatusMessage"
        tone="warning"
      >
        <div class="swipe-view__message-actions">
          <RouterLink
            :to="{ name: 'media-list' }"
            class="button swipe-view__message-button"
          >
            {{ t('swipe.rateMoreMedia') }}
          </RouterLink>
          <RouterLink
            :to="{ name: 'external-search' }"
            class="button swipe-view__message-button swipe-view__message-button--ghost"
          >
            {{ t('swipe.importTitles') }}
          </RouterLink>
        </div>
      </AppMessage>

      <template v-if="showProfileWarmupEmptyState">
        <AppMessage
          :title="t('swipe.warmupEmptyTitle')"
          :description="profileStatusMessage"
          tone="warning"
        >
          <div class="swipe-view__message-actions">
            <RouterLink
              :to="{ name: 'media-list' }"
              class="button swipe-view__message-button"
            >
              {{ t('swipe.rateMedia') }}
            </RouterLink>
            <RouterLink
              :to="{ name: 'external-search' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              {{ t('swipe.import') }}
            </RouterLink>
          </div>
        </AppMessage>
      </template>

      <template v-else-if="showNoRecommendationsState">
        <AppMessage
          :title="t('swipe.emptyTitle')"
          :description="t('swipe.emptyDescription')"
        >
          <div class="swipe-view__message-actions">
            <RouterLink
              :to="{ name: 'media-create' }"
              class="button swipe-view__message-button"
            >
              {{ t('swipe.createTitle') }}
            </RouterLink>
            <RouterLink
              :to="{ name: 'external-search' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              {{ t('swipe.importExternal') }}
            </RouterLink>
            <RouterLink
              :to="{ name: 'candidates' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              {{ t('swipe.reviewCandidates') }}
            </RouterLink>
          </div>
        </AppMessage>
      </template>

      <template v-else>
        <SwipeProgress
          :total-count="totalCount"
          :remaining-count="remainingCount"
          :stats="stats"
        />

        <AppMessage
          v-if="showDoneState"
          :title="t('swipe.doneTitle')"
          :description="t('swipe.doneDescription', { liked: stats.liked, skipped: stats.skipped })"
          tone="info"
        >
          <div class="swipe-view__message-actions">
            <button
              class="button swipe-view__message-button"
              type="button"
              @click="loadQueue"
            >
              {{ t('swipe.reloadRound') }}
            </button>
            <RouterLink
              :to="{ name: 'external-search' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              {{ t('swipe.fetchNewTitles') }}
            </RouterLink>
          </div>
        </AppMessage>

        <section
          v-else-if="currentItem"
          class="swipe-view__deck"
        >
          <div class="swipe-view__card-column">
            <SwipeCandidateCard
              ref="activeCardRef"
              :item="currentItem"
              :next-item="nextItem"
              :match-insights-available="matchInsightsAvailable"
              :profile-ready="profileReady"
              :scores-suppressed="scoresSuppressed"
              :details-expanded="detailsExpanded"
              :interaction-locked="interactionLocked"
              @decision-request="handleGestureDecision"
              @toggle-details="handleDetailsToggle"
              @details-load-error="handleDetailsLoadError"
            />

            <SwipeDecisionControls
              :pending="interactionLocked"
              :details-expanded="detailsExpanded"
              @like="handleLike"
              @skip="handleSkip"
              @details="handleDetailsToggle"
            />
          </div>
        </section>
      </template>
    </template>

    <p class="swipe-view__footnote">
      {{ t('swipe.footnote') }}
    </p>
  </section>
</template>

<style scoped>
.swipe-view {
  position: relative;
  display: grid;
  gap: 1.25rem;
  padding: clamp(1rem, 2vw, 1.6rem);
  border-radius: 2rem;
  overflow: hidden;
  background: var(--theme-swipe-background);
  color: var(--theme-swipe-text);
  box-shadow: 0 28px 60px rgba(8, 15, 32, 0.28);
}

.swipe-view::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--theme-swipe-overlay);
  pointer-events: none;
}

.swipe-view__header,
.swipe-view__deck,
.swipe-view__footnote,
.swipe-view__sr-only {
  position: relative;
  z-index: 1;
}

.swipe-view__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.swipe-view__header-copy {
  display: grid;
  gap: 0.45rem;
  max-width: 38rem;
}

.swipe-view__eyebrow,
.swipe-view__title,
.swipe-view__copy,
.swipe-view__footnote {
  margin: 0;
}

.swipe-view__eyebrow {
  color: var(--theme-swipe-eyebrow-color);
  font-size: 0.86rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.swipe-view__title {
  color: var(--theme-swipe-title-color);
  font-size: clamp(2rem, 5vw, 3rem);
  line-height: 0.98;
}

.swipe-view__copy,
.swipe-view__footnote {
  color: var(--theme-swipe-muted-text);
}

.swipe-view__actions,
.swipe-view__message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.swipe-view__action-button,
.swipe-view__message-button {
  min-height: 2.9rem;
  border-color: var(--theme-swipe-action-border);
  background: var(--theme-swipe-action-background);
  color: var(--theme-swipe-panel-pill-color);
  backdrop-filter: blur(14px);
}

.swipe-view__action-button--ghost,
.swipe-view__message-button--ghost {
  background: var(--theme-swipe-action-background-ghost);
}

.swipe-view__deck {
  display: grid;
  place-items: center;
}

.swipe-view__card-column {
  width: min(100%, 27rem);
  display: grid;
  gap: 0.85rem;
}

.swipe-view__footnote {
  font-size: 0.92rem;
}

.swipe-view__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.swipe-view :deep(.app-message) {
  position: relative;
  z-index: 1;
  padding: 1.1rem 1.15rem;
  border-color: var(--theme-swipe-panel-border);
  background: var(--theme-swipe-panel-background);
  box-shadow: none;
  backdrop-filter: blur(18px);
}

.swipe-view :deep(.app-message__title) {
  color: var(--theme-swipe-panel-emphasis-text);
}

.swipe-view :deep(.app-message__description) {
  color: var(--theme-swipe-muted-text);
}

.swipe-view :deep(.app-message--warning) {
  background: var(--theme-swipe-message-warning-background);
}

.swipe-view :deep(.app-message--error) {
  background: var(--theme-swipe-message-error-background);
}

.swipe-view :deep(.app-message--info) {
  background: var(--theme-swipe-message-info-background);
}

@media (min-width: 960px) {
  .swipe-view__card-column {
    width: min(100%, 26.5rem);
  }
}

@media (max-width: 720px) {
  .swipe-view {
    padding: 0.95rem;
    border-radius: 1.7rem;
  }

  .swipe-view__actions {
    width: 100%;
  }

  .swipe-view__action-button {
    flex: 1 1 0;
  }
}

@media (max-width: 480px) {
  .swipe-view__header {
    gap: 0.85rem;
  }

  .swipe-view__title {
    font-size: 1.85rem;
  }

  .swipe-view__message-actions {
    display: grid;
  }

  .swipe-view__message-button {
    width: 100%;
  }
}
</style>
