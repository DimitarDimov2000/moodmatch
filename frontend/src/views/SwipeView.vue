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
  SwipeGestureIntent,
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
const feedbackIntent = ref<SwipeGestureIntent>('none');
const feedbackIntensity = ref(0);
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
const likeDestinationStyle = computed(() => ({
  opacity: feedbackIntent.value === 'like' ? String(feedbackIntensity.value) : '0',
}));
const skipDestinationStyle = computed(() => ({
  opacity: feedbackIntent.value === 'skip' ? String(feedbackIntensity.value) : '0',
}));

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
  feedbackIntent.value = 'none';
  feedbackIntensity.value = 0;
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

function handleFeedbackChange(feedback: {
  intent: SwipeGestureIntent;
  intensity: number;
}) {
  feedbackIntent.value = feedback.intent;
  feedbackIntensity.value = feedback.intensity;
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
          <div
            class="swipe-view__destination-zone swipe-view__destination-zone--skip"
            :style="skipDestinationStyle"
            aria-hidden="true"
          >
            <div class="swipe-view__destination-marker">
              <span class="swipe-view__destination-icon">×</span>
              <span class="swipe-view__destination-copy">
                <strong>{{ t('swipeCards.dragSkipMain') }}</strong>
                <small>{{ t('swipeCards.dragSkipSecondary') }}</small>
              </span>
            </div>
          </div>

          <div
            class="swipe-view__destination-zone swipe-view__destination-zone--like"
            :style="likeDestinationStyle"
            aria-hidden="true"
          >
            <div class="swipe-view__destination-marker">
              <span class="swipe-view__destination-icon">✓</span>
              <span class="swipe-view__destination-copy">
                <strong>{{ t('swipeCards.dragLikeMain') }}</strong>
                <small>{{ t('swipeCards.dragLikeSecondary') }}</small>
              </span>
            </div>
          </div>

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
              @feedback-change="handleFeedbackChange"
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
  </section>
</template>

<style scoped>
.swipe-view {
  position: relative;
  display: grid;
  gap: 1rem;
  padding: clamp(0.9rem, 1.7vw, 1.3rem);
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
.swipe-view__sr-only {
  position: relative;
  z-index: 1;
}

.swipe-view__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem 1rem;
}

.swipe-view__header-copy {
  display: grid;
  gap: 0.28rem;
  max-width: 34rem;
}

.swipe-view__title,
.swipe-view__copy,
.swipe-view__footnote {
  margin: 0;
}

.swipe-view__title {
  color: var(--theme-swipe-title-color);
  font-size: clamp(1.65rem, 3.4vw, 2.25rem);
  line-height: 1.02;
}

.swipe-view__copy,
.swipe-view__footnote {
  color: var(--theme-swipe-muted-text);
}

.swipe-view__copy {
  max-width: 54ch;
  font-size: 0.92rem;
  line-height: 1.45;
}

.swipe-view__actions,
.swipe-view__message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.swipe-view__action-button,
.swipe-view__message-button {
  min-height: 2.65rem;
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
  position: relative;
  isolation: isolate;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--theme-swipe-panel-border) 58%, transparent);
  border-radius: 1.85rem;
  background: color-mix(in srgb, var(--theme-swipe-panel-background) 34%, transparent);
}

.swipe-view__card-column {
  position: relative;
  z-index: 1;
  width: min(100%, 32rem);
  display: grid;
  gap: 0.85rem;
}

.swipe-view__destination-zone {
  position: absolute;
  inset-block: 0;
  z-index: 0;
  display: flex;
  width: 50%;
  min-height: 100%;
  align-items: center;
  padding-inline: clamp(0.95rem, 2.8vw, 2.3rem);
  opacity: 0;
  pointer-events: none;
  transition: opacity 90ms linear;
}

.swipe-view__destination-zone--skip {
  left: 0;
  justify-content: flex-start;
  background:
    radial-gradient(
      ellipse at 10% 20%,
      color-mix(in srgb, #db6178 54%, transparent),
      transparent 52%
    ),
    radial-gradient(
      ellipse at 0% 84%,
      color-mix(in srgb, #c64c66 32%, transparent),
      transparent 54%
    ),
    linear-gradient(
      90deg,
      color-mix(in srgb, #7f2237 58%, var(--theme-swipe-panel-background)),
      color-mix(in srgb, #7f2237 10%, transparent) 62%
    );
  box-shadow:
    inset 1px 0 color-mix(in srgb, #ef7086 36%, transparent),
    inset 4rem 0 7rem color-mix(in srgb, #b83f54 26%, transparent),
    inset 0 3.4rem 5rem -4rem color-mix(in srgb, #f08ca0 22%, transparent),
    inset 0 -3.4rem 5rem -4rem color-mix(in srgb, #f08ca0 18%, transparent);
  color: #fff1f4;
  text-align: left;
}

.swipe-view__destination-zone--like {
  right: 0;
  justify-content: flex-end;
  background:
    radial-gradient(
      ellipse at 90% 20%,
      color-mix(in srgb, #32bf8e 52%, transparent),
      transparent 52%
    ),
    radial-gradient(
      ellipse at 100% 84%,
      color-mix(in srgb, #209b73 30%, transparent),
      transparent 54%
    ),
    linear-gradient(
      270deg,
      color-mix(in srgb, #125c47 58%, var(--theme-swipe-panel-background)),
      color-mix(in srgb, #125c47 10%, transparent) 62%
    );
  box-shadow:
    inset -1px 0 color-mix(in srgb, #62d8ad 36%, transparent),
    inset -4rem 0 7rem color-mix(in srgb, #17845f 26%, transparent),
    inset 0 3.4rem 5rem -4rem color-mix(in srgb, #76e1b8 20%, transparent),
    inset 0 -3.4rem 5rem -4rem color-mix(in srgb, #76e1b8 16%, transparent);
  color: #effff8;
  text-align: right;
}

.swipe-view__destination-zone::before {
  content: '';
  position: absolute;
  inset-block: 6%;
  width: 0.46rem;
  border-radius: var(--radius-full);
  filter: blur(10px);
  opacity: 0.94;
  pointer-events: none;
}

.swipe-view__destination-zone--skip::before {
  left: 0.1rem;
  background: linear-gradient(180deg, rgba(255, 169, 187, 0.96), rgba(209, 75, 99, 0.2));
  box-shadow: 0 0 1.6rem rgba(209, 75, 99, 0.64);
}

.swipe-view__destination-zone--like::before {
  right: 0.1rem;
  background: linear-gradient(180deg, rgba(149, 255, 214, 0.96), rgba(26, 170, 121, 0.2));
  box-shadow: 0 0 1.6rem rgba(26, 170, 121, 0.62);
}

.swipe-view__destination-zone::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.12),
    transparent 20%,
    transparent 80%,
    rgba(255, 255, 255, 0.08)
  );
  mix-blend-mode: screen;
  opacity: 0.36;
  pointer-events: none;
}

.swipe-view__destination-marker {
  display: grid;
  gap: 0.18rem;
  width: min(10.5rem, calc(100% - 0.4rem));
  padding: 0;
  justify-items: center;
  text-align: center;
  filter: drop-shadow(0 0 1.2rem rgba(255, 255, 255, 0.28));
}

.swipe-view__destination-zone--skip .swipe-view__destination-marker {
  justify-items: center;
}

.swipe-view__destination-zone--like .swipe-view__destination-marker {
  justify-items: center;
}

.swipe-view__destination-icon {
  font-size: clamp(4.35rem, 9vw, 6.25rem);
  font-weight: 800;
  line-height: 0.72;
  color: rgba(255, 255, 255, 1);
  opacity: 1;
  text-shadow:
    0 0 0.75rem rgba(255, 255, 255, 0.9),
    0 0 2.1rem rgba(255, 255, 255, 0.76),
    0 0 4.4rem currentColor;
}

.swipe-view__destination-zone--skip .swipe-view__destination-icon {
  transform: translateX(-0.08em) rotate(-6deg);
}

.swipe-view__destination-zone--like .swipe-view__destination-icon {
  transform: translateX(0.08em) rotate(6deg);
}

.swipe-view__destination-copy {
  display: grid;
  gap: 0.06rem;
  max-width: 100%;
  justify-items: center;
  text-align: center;
  color: rgba(255, 255, 255, 0.96);
  text-shadow:
    0 0 0.68rem rgba(255, 255, 255, 0.38),
    0 2px 15px rgba(8, 15, 32, 0.5);
}

.swipe-view__destination-zone--skip .swipe-view__destination-copy {
  justify-items: center;
}

.swipe-view__destination-zone--like .swipe-view__destination-copy {
  justify-items: center;
}

.swipe-view__destination-copy strong {
  font-size: clamp(0.76rem, 1.32vw, 0.92rem);
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  opacity: 0.96;
}

.swipe-view__destination-copy small {
  font-size: clamp(0.6rem, 1vw, 0.7rem);
  letter-spacing: 0.07em;
  text-transform: uppercase;
  opacity: 0.66;
}

.swipe-view__card-column :deep(.swipe-decision-controls) {
  width: min(calc(100% - 4rem), 25rem);
  margin-inline: auto;
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
    width: min(100%, 34rem);
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

  .swipe-view__card-column :deep(.swipe-decision-controls) {
    width: min(calc(100% - 1rem), 25rem);
  }

  .swipe-view__destination-marker {
    width: min(8.1rem, calc(100% - 0.2rem));
  }

  .swipe-view__destination-icon {
    font-size: clamp(2.55rem, 13vw, 3.55rem);
  }
}

@media (prefers-reduced-motion: reduce) {
  .swipe-view__destination-zone {
    transition-duration: 1ms;
  }
}

@media (max-width: 480px) {
  .swipe-view__header {
    gap: 0.85rem;
  }

  .swipe-view__title {
    font-size: 1.65rem;
  }

  .swipe-view__message-actions {
    display: grid;
  }

  .swipe-view__message-button {
    width: 100%;
  }
}
</style>
