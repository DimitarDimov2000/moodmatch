<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { i18n } from '@/i18n';
import { getMediaById } from '@/api/media';
import { sourceTypeLabels } from '@/components/media/media-options';
import { getExternalSourceLabel } from '@/components/media/media-presentation';
import {
  formatDecimal,
  mediaTypeLabels,
} from '@/components/matching/matching-format';
import {
  summarizeSwipeInsight,
  type SwipeInsightSummary,
} from '@/components/swipe/swipe-insights';
import TagChip from '@/components/tags/TagChip.vue';
import type {
  MediaResponse,
} from '@/types/api';
import type {
  SwipeDecisionAction,
  SwipeGestureIntent,
  SwipeQueueItem,
} from '@/types/swipe';

const INTENT_THRESHOLD = 24;
const HORIZONTAL_THRESHOLD = 100;
const HORIZONTAL_CLAMP = 170;
const VERTICAL_CLAMP = 42;
const DESCRIPTION_PREVIEW_LIMIT = 260;

const props = withDefaults(
  defineProps<{
    item: SwipeQueueItem;
    nextItem?: SwipeQueueItem | null;
    matchInsightsAvailable: boolean;
    profileReady?: boolean | null;
    scoresSuppressed?: boolean;
    detailsExpanded?: boolean;
    interactionLocked?: boolean;
  }>(),
  {
    nextItem: null,
    profileReady: null,
    scoresSuppressed: false,
    detailsExpanded: false,
    interactionLocked: false,
  },
);

const emit = defineEmits<{
  decisionRequest: [action: SwipeDecisionAction];
  toggleDetails: [];
  detailsLoadError: [message: string];
}>();
const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;

const prefersReducedMotion = ref(false);
const dragging = ref(false);
const awaitingDecision = ref(false);
const exiting = ref(false);
const dragX = ref(0);
const dragY = ref(0);
const startX = ref(0);
const startY = ref(0);
const activePointerId = ref<number | null>(null);
const detailsLoading = ref(false);
const detailsMedia = ref<MediaResponse | null>(null);
const detailsErrorMessage = ref('');
const coverImageReady = ref(false);
const coverImageFailed = ref(false);
const peekCoverImageReady = ref(false);
const peekCoverImageFailed = ref(false);

let detailsRequestVersion = 0;

const insight = computed<SwipeInsightSummary>(() =>
  {
    trackLocaleDependency();
    return summarizeSwipeInsight(props.item, {
      matchInsightsAvailable: props.matchInsightsAvailable,
      profileReady: props.profileReady ?? null,
      scoresSuppressed: props.scoresSuppressed ?? false,
    });
  });
const detailsId = computed(() => `swipe-details-${props.item.candidate.media.id}`);
const metaLine = computed(() => {
  trackLocaleDependency();
  const parts = [mediaTypeLabels[props.item.candidate.media.mediaType]];

  if (props.item.candidate.media.releaseYear) {
    parts.push(String(props.item.candidate.media.releaseYear));
  }

  return parts.join(' · ');
});
const gradientSeed = computed(() => getGradientSeed(props.item.candidate.media.id));
const reasonChips = computed(() => insight.value.reasonChips);
const matchingTags = computed(() => props.item.match?.matchingTags ?? []);
const extraCandidateTags = computed(() => props.item.match?.extraCandidateTags ?? []);
const technicalDetailsAvailable = computed(
  () =>
    Boolean(props.item.match) &&
    (
      props.item.match?.rawScore !== null ||
      props.item.match?.adjustedScore !== null ||
      props.item.match?.precisionFactor !== null ||
      props.item.match?.relativeScore !== null
    ),
);
const technicalScoreRows = computed(() => {
  trackLocaleDependency();
  return [
    {
      label: t('swipeCards.technicalMatchingTags'),
      value: props.item.match
        ? `${props.item.match.matchingTagCount} / ${props.item.match.candidateTagCount}`
        : t('swipeCards.unavailable'),
    },
    {
      label: t('swipeCards.technicalAdjustedScore'),
      value: formatDecimal(props.item.match?.adjustedScore ?? null) ?? t('swipeCards.unavailable'),
    },
    {
      label: t('swipeCards.technicalRawScore'),
      value: formatDecimal(props.item.match?.rawScore ?? null) ?? t('swipeCards.unavailable'),
    },
    {
      label: t('swipeCards.technicalPrecision'),
      value: formatDecimal(props.item.match?.precisionFactor ?? null) ?? t('swipeCards.unavailable'),
    },
  ];
});
const descriptionPreview = computed(() => trimText(detailsMedia.value?.description ?? null, DESCRIPTION_PREVIEW_LIMIT));
const sourceLabel = computed(() => {
  const media = detailsMedia.value;

  if (!media) {
    return null;
  }

  if (media.externalSourceName) {
    return getExternalSourceLabel(media.externalSourceName);
  }

  const reference = media.externalReferences.find((entry) => entry.externalUrl || entry.sourceName);
  if (reference) {
    return getExternalSourceLabel(reference.sourceName);
  }

  if (media.sourceType !== 'MANUAL') {
    return sourceTypeLabels[media.sourceType];
  }

  return null;
});
const sourceUrl = computed(() => {
  const media = detailsMedia.value;

  if (!media) {
    return null;
  }

  return media.externalSourceUrl ?? media.externalReferences.find((entry) => entry.externalUrl)?.externalUrl ?? null;
});
const gestureIntent = computed<SwipeGestureIntent>(() => getGestureIntent(dragX.value, dragY.value));
const showIntentIndicators = computed(
  () => dragging.value || awaitingDecision.value || exiting.value,
);
const showPeekCard = computed(
  () => Boolean(props.nextItem) && showIntentIndicators.value,
);
const cardTransitionDuration = computed(() => {
  if (dragging.value) {
    return '0ms';
  }

  return prefersReducedMotion.value ? '40ms' : '220ms';
});
const cardRotation = computed(() => clamp(dragX.value / 18, -8, 8));
const cardOpacity = computed(() => clamp(1 - Math.abs(dragX.value) / 520, 0.78, 1));
const cardStyle = computed(() => ({
  opacity: String(cardOpacity.value),
  transform: `translate3d(${dragX.value}px, ${dragY.value}px, 0) rotate(${cardRotation.value}deg)`,
  transitionDuration: cardTransitionDuration.value,
}));

onMounted(() => {
  prefersReducedMotion.value =
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
});

watch(
  () => props.item.candidate.media.id,
  () => {
    detailsRequestVersion += 1;
    detailsLoading.value = false;
    detailsMedia.value = null;
    detailsErrorMessage.value = '';
    coverImageReady.value = false;
    coverImageFailed.value = false;
    resetGesturePosition();
  },
);

watch(
  () => props.nextItem?.candidate.media.id ?? null,
  () => {
    peekCoverImageReady.value = false;
    peekCoverImageFailed.value = false;
  },
  { immediate: true },
);

watch(
  () => props.detailsExpanded,
  (expanded) => {
    if (expanded) {
      void ensureDetailsLoaded();
    }
  },
);

function handlePointerDown(event: PointerEvent) {
  if (
    props.interactionLocked ||
    awaitingDecision.value ||
    exiting.value ||
    isInteractiveTarget(event.target)
  ) {
    return;
  }

  activePointerId.value = event.pointerId;
  startX.value = event.clientX;
  startY.value = event.clientY;
  dragging.value = true;
  dragX.value = 0;
  dragY.value = 0;

  const currentTarget = event.currentTarget;
  if (currentTarget instanceof Element && 'setPointerCapture' in currentTarget) {
    currentTarget.setPointerCapture(event.pointerId);
  }
}

function handlePointerMove(event: PointerEvent) {
  if (!dragging.value || event.pointerId !== activePointerId.value) {
    return;
  }

  const rawX = event.clientX - startX.value;
  const rawY = event.clientY - startY.value;
  const horizontalDominant = Math.abs(rawX) > Math.abs(rawY) * 0.95;

  dragX.value = clamp(
    horizontalDominant ? rawX : rawX * 0.35,
    -HORIZONTAL_CLAMP,
    HORIZONTAL_CLAMP,
  );
  dragY.value = clamp(rawY * 0.12, -VERTICAL_CLAMP, VERTICAL_CLAMP);
}

function handlePointerUp(event: PointerEvent) {
  if (!dragging.value || event.pointerId !== activePointerId.value) {
    return;
  }

  releasePointer(event);

  if (gestureIntent.value === 'like' && Math.abs(dragX.value) >= HORIZONTAL_THRESHOLD) {
    awaitDecision('like');
    return;
  }

  if (gestureIntent.value === 'skip' && Math.abs(dragX.value) >= HORIZONTAL_THRESHOLD) {
    awaitDecision('skip');
    return;
  }

  resetGesturePosition();
}

function handlePointerCancel(event: PointerEvent) {
  if (event.pointerId !== activePointerId.value) {
    return;
  }

  releasePointer(event);
  resetGesturePosition();
}

function releasePointer(event: PointerEvent) {
  dragging.value = false;

  const currentTarget = event.currentTarget;
  if (currentTarget instanceof Element && 'releasePointerCapture' in currentTarget) {
    if (currentTarget.hasPointerCapture?.(event.pointerId)) {
      currentTarget.releasePointerCapture(event.pointerId);
    }
  }

  activePointerId.value = null;
}

function awaitDecision(action: SwipeDecisionAction) {
  dragging.value = false;
  awaitingDecision.value = true;
  emit('decisionRequest', action);
}

async function playDecisionAnimation(action: SwipeDecisionAction): Promise<void> {
  awaitingDecision.value = true;
  exiting.value = true;
  dragX.value = action === 'like' ? getExitDistance() : -getExitDistance();
  dragY.value = 0;

  await wait(prefersReducedMotion.value ? 40 : 220);
}

function resetGesturePosition() {
  dragging.value = false;
  awaitingDecision.value = false;
  exiting.value = false;
  activePointerId.value = null;
  dragX.value = 0;
  dragY.value = 0;
}

function toggleDetails() {
  emit('toggleDetails');
}

function handleCoverImageLoad() {
  coverImageReady.value = true;
}

function handleCoverImageError() {
  coverImageFailed.value = true;
}

function handlePeekCoverImageLoad() {
  peekCoverImageReady.value = true;
}

function handlePeekCoverImageError() {
  peekCoverImageFailed.value = true;
}

async function ensureDetailsLoaded() {
  if (detailsMedia.value || detailsLoading.value) {
    return;
  }

  const requestVersion = ++detailsRequestVersion;
  detailsLoading.value = true;
  detailsErrorMessage.value = '';

  try {
    const media = await getMediaById(props.item.candidate.media.id);

    if (requestVersion !== detailsRequestVersion) {
      return;
    }

    detailsMedia.value = media;
  } catch (error) {
    if (requestVersion !== detailsRequestVersion) {
      return;
    }

    const message = toDetailsErrorMessage(error);
    detailsErrorMessage.value = message;
    emit('detailsLoadError', message);
  } finally {
    if (requestVersion === detailsRequestVersion) {
      detailsLoading.value = false;
    }
  }
}

function getGestureIntent(x: number, y: number): SwipeGestureIntent {
  const absoluteX = Math.abs(x);

  if (absoluteX < INTENT_THRESHOLD) {
    return 'none';
  }

  if (absoluteX <= Math.abs(y) * 1.15) {
    return 'none';
  }

  return x >= 0 ? 'like' : 'skip';
}

function isInteractiveTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }

  return Boolean(target.closest('button, a, input, select, textarea, summary, [contenteditable="true"]'));
}

function toDetailsErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return t('swipeCards.detailsError');
}

function getGradientSeed(value: string): string {
  const seed = Array.from(value).reduce((total, character) => total + character.charCodeAt(0), 0);

  if (seed % 3 === 0) {
    return 'swipe-candidate-card__cover--sunrise';
  }

  if (seed % 3 === 1) {
    return 'swipe-candidate-card__cover--ocean';
  }

  return 'swipe-candidate-card__cover--mint';
}

function trimText(value: string | null, limit: number): string | null {
  if (!value) {
    return null;
  }

  const normalized = value.trim();
  if (normalized.length <= limit) {
    return normalized;
  }

  return `${normalized.slice(0, limit).trimEnd()}...`;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function getExitDistance(): number {
  if (typeof window === 'undefined') {
    return 460;
  }

  return Math.max(window.innerWidth * 1.1, 460);
}

function wait(duration: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, duration);
  });
}

defineExpose({
  playDecisionAnimation,
  resetGesturePosition,
});
</script>

<template>
  <div class="swipe-candidate-card__stage">
    <article
      v-if="nextItem"
      class="swipe-candidate-card__peek page-card"
      :class="{ 'swipe-candidate-card__peek--visible': showPeekCard }"
      aria-hidden="true"
    >
      <div
        class="swipe-candidate-card__peek-cover"
        :class="getGradientSeed(nextItem.candidate.media.id)"
      >
        <img
          v-if="nextItem.candidate.media.coverUrl && !peekCoverImageFailed"
          class="swipe-candidate-card__peek-cover-image"
          :class="{ 'swipe-candidate-card__peek-cover-image--ready': peekCoverImageReady }"
          :src="nextItem.candidate.media.coverUrl"
          :alt="t('swipeCards.coverAlt', { title: nextItem.candidate.media.title })"
          @load="handlePeekCoverImageLoad"
          @error="handlePeekCoverImageError"
        >
        <div
          v-if="!nextItem.candidate.media.coverUrl || peekCoverImageFailed || !peekCoverImageReady"
          class="swipe-candidate-card__peek-cover-fallback"
          aria-hidden="true"
        >
          {{ mediaTypeLabels[nextItem.candidate.media.mediaType] }}
        </div>

        <div class="swipe-candidate-card__peek-cover-overlay">
          <div class="swipe-candidate-card__peek-top">
            <span class="swipe-candidate-card__peek-type-pill">
              {{ mediaTypeLabels[nextItem.candidate.media.mediaType] }}
            </span>
            <span class="swipe-candidate-card__peek-next-pill">
              {{ t('swipeCards.next') }}
            </span>
          </div>

          <div class="swipe-candidate-card__peek-hero">
            <p class="swipe-candidate-card__peek-eyebrow">
              {{ t('swipeCards.recommendation') }}
            </p>
            <h3 class="swipe-candidate-card__peek-title">
              {{ nextItem.candidate.media.title }}
            </h3>
            <p class="swipe-candidate-card__peek-meta">
              {{ mediaTypeLabels[nextItem.candidate.media.mediaType] }}
              <span v-if="nextItem.candidate.media.releaseYear">
                · {{ nextItem.candidate.media.releaseYear }}
              </span>
            </p>
          </div>
        </div>
      </div>
    </article>

    <div
      class="swipe-candidate-card__intents"
      :class="{ 'swipe-candidate-card__intents--visible': showIntentIndicators }"
      aria-hidden="true"
    >
      <span
        class="swipe-candidate-card__intent swipe-candidate-card__intent--skip"
        :class="{ 'swipe-candidate-card__intent--active': gestureIntent === 'skip' }"
      >
        {{ t('swipeCards.intentSkip') }}
      </span>
      <span
        class="swipe-candidate-card__intent swipe-candidate-card__intent--like"
        :class="{ 'swipe-candidate-card__intent--active': gestureIntent === 'like' }"
      >
        {{ t('swipeCards.intentLike') }}
      </span>
    </div>

    <article
      class="swipe-candidate-card page-card"
      :class="{
        'swipe-candidate-card--dragging': dragging,
        'swipe-candidate-card--locked': interactionLocked || awaitingDecision,
      }"
      :style="cardStyle"
    >
      <div
        class="swipe-candidate-card__swipe-surface"
        @pointerdown="handlePointerDown"
        @pointermove="handlePointerMove"
        @pointerup="handlePointerUp"
        @pointercancel="handlePointerCancel"
      >
        <div
          class="swipe-candidate-card__cover"
          :class="gradientSeed"
        >
          <img
            v-if="item.candidate.media.coverUrl && !coverImageFailed"
            class="swipe-candidate-card__cover-image"
            :class="{ 'swipe-candidate-card__cover-image--ready': coverImageReady }"
            :src="item.candidate.media.coverUrl"
            :alt="t('swipeCards.coverAlt', { title: item.candidate.media.title })"
            @load="handleCoverImageLoad"
            @error="handleCoverImageError"
          >
          <div
            v-if="!item.candidate.media.coverUrl || coverImageFailed || !coverImageReady"
            class="swipe-candidate-card__cover-fallback"
            aria-hidden="true"
          >
            {{ mediaTypeLabels[item.candidate.media.mediaType] }}
          </div>

          <div class="swipe-candidate-card__cover-overlay">
            <div class="swipe-candidate-card__hero-top">
              <span class="swipe-candidate-card__type-pill">
                {{ mediaTypeLabels[item.candidate.media.mediaType] }}
              </span>

              <div
                class="swipe-candidate-card__score-badge"
                :class="`swipe-candidate-card__score-badge--${insight.valueTone}`"
              >
                <span class="swipe-candidate-card__score-label">{{ insight.valueLabel }}</span>
                <span class="swipe-candidate-card__score-caption">{{ insight.valueCaption }}</span>
              </div>
            </div>

            <div class="swipe-candidate-card__hero-copy">
              <p class="swipe-candidate-card__eyebrow">
                {{ t('swipeCards.recommendation') }}
              </p>
              <h2 class="swipe-candidate-card__title">
                {{ item.candidate.media.title }}
              </h2>
              <p class="swipe-candidate-card__meta">
                {{ metaLine }}
              </p>
            </div>
          </div>
        </div>

        <div class="swipe-candidate-card__body">
          <p class="swipe-candidate-card__headline">
            {{ insight.headline }}
          </p>
          <p class="swipe-candidate-card__supporting-copy">
            {{ insight.supportingCopy }}
          </p>

          <div
            class="swipe-candidate-card__reason-chips"
            :aria-label="t('swipeCards.reasonList')"
          >
            <span
              v-for="chip in reasonChips"
              :key="chip"
              class="swipe-candidate-card__reason-chip"
            >
              {{ chip }}
            </span>
          </div>

          <div class="swipe-candidate-card__affordance">
            <p class="swipe-candidate-card__affordance-copy">
              {{ t('swipeCards.affordance') }}
            </p>

            <button
              class="button button--ghost swipe-candidate-card__details-button"
              type="button"
              :aria-controls="detailsId"
              :aria-expanded="detailsExpanded ? 'true' : 'false'"
              @click="toggleDetails"
            >
              {{ detailsExpanded ? t('swipeCards.detailsLess') : t('swipeCards.detailsMore') }}
            </button>
          </div>
        </div>
      </div>

      <section
        v-if="detailsExpanded"
        :id="detailsId"
        class="swipe-candidate-card__details-panel"
      >
        <div class="swipe-candidate-card__details-header">
          <div>
            <p class="swipe-candidate-card__details-eyebrow">
              {{ t('swipeCards.detailsEyebrow') }}
            </p>
            <h3 class="swipe-candidate-card__details-title">
              {{ t('swipeCards.detailsTitle') }}
            </h3>
          </div>

          <RouterLink
            class="button button--secondary swipe-candidate-card__open-link"
            :to="{
              name: 'media-detail',
              params: {
                id: item.candidate.media.id,
              },
            }"
          >
            {{ t('swipeCards.openFullDetails') }}
          </RouterLink>
        </div>

        <div class="swipe-candidate-card__details-grid">
          <section class="swipe-candidate-card__details-block">
            <p class="swipe-candidate-card__details-label">
              {{ t('swipeCards.whyShown') }}
            </p>
            <p class="swipe-candidate-card__details-copy">
              {{ insight.detailsIntro }}
            </p>
            <p class="swipe-candidate-card__details-note">
              {{
                props.matchInsightsAvailable
                  ? t('swipeCards.logicReady')
                  : t('swipeCards.logicLoading')
              }}
            </p>
          </section>

          <section class="swipe-candidate-card__details-block">
            <p class="swipe-candidate-card__details-label">
              {{ t('swipeCards.tagsReasons') }}
            </p>

            <p
              v-if="!matchingTags.length && !extraCandidateTags.length"
              class="swipe-candidate-card__details-note"
            >
              {{ t('swipeCards.noDetailedTags') }}
            </p>

            <div
              v-if="matchingTags.length"
              class="swipe-candidate-card__tag-group"
            >
              <p class="swipe-candidate-card__tag-group-title">
                {{ t('swipeCards.directHits') }}
              </p>
              <div class="swipe-candidate-card__tag-list">
                <TagChip
                  v-for="entry in matchingTags"
                  :key="entry.tag.id"
                  :tag="entry.tag"
                />
              </div>
            </div>

            <div
              v-if="extraCandidateTags.length"
              class="swipe-candidate-card__tag-group"
            >
              <p class="swipe-candidate-card__tag-group-title">
                {{ t('swipeCards.extraVibes') }}
              </p>
              <div class="swipe-candidate-card__tag-list">
                <TagChip
                  v-for="tag in extraCandidateTags"
                  :key="tag.id"
                  :tag="tag"
                />
              </div>
            </div>
          </section>

          <section class="swipe-candidate-card__details-block">
            <p class="swipe-candidate-card__details-label">
              {{ t('swipeCards.description') }}
            </p>

            <p
              v-if="detailsLoading"
              class="swipe-candidate-card__details-note"
            >
              {{ t('swipeCards.loadingContext') }}
            </p>
            <p
              v-else-if="detailsErrorMessage"
              class="swipe-candidate-card__details-note swipe-candidate-card__details-note--error"
            >
              {{ detailsErrorMessage }}
            </p>
            <p
              v-else-if="descriptionPreview"
              class="swipe-candidate-card__details-copy"
            >
              {{ descriptionPreview }}
            </p>
            <p
              v-else
              class="swipe-candidate-card__details-note"
            >
              {{ t('swipeCards.noDescription') }}
            </p>

            <p
              v-if="sourceLabel"
              class="swipe-candidate-card__details-source"
            >
              {{ t('swipeCards.source') }}
              <a
                v-if="sourceUrl"
                :href="sourceUrl"
                target="_blank"
                rel="noreferrer"
              >
                {{ sourceLabel }}
              </a>
              <span v-else>{{ sourceLabel }}</span>
            </p>
          </section>
        </div>

        <details
          v-if="technicalDetailsAvailable"
          class="swipe-candidate-card__technical"
        >
          <summary>{{ t('swipeCards.technicalTitle') }}</summary>

          <dl class="swipe-candidate-card__technical-grid">
            <div
              v-for="row in technicalScoreRows"
              :key="row.label"
            >
              <dt>{{ row.label }}</dt>
              <dd>{{ row.value }}</dd>
            </div>
          </dl>
        </details>
      </section>
    </article>
  </div>
</template>

<style scoped>
.swipe-candidate-card__stage {
  position: relative;
  padding: 0 0.85rem 1.15rem 0;
  overflow: visible;
}

.swipe-candidate-card__peek {
  position: absolute;
  inset: 0.9rem 0 0 0.95rem;
  z-index: 0;
  padding: 0;
  opacity: 0;
  transform: translate3d(0.5rem, 0.6rem, 0) scale(0.978);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--color-surface-secondary) 84%, var(--color-accent-soft)),
    color-mix(in srgb, var(--color-surface) 88%, transparent)
  );
  box-shadow: 0 18px 34px rgba(8, 15, 32, 0.14);
  backdrop-filter: blur(12px);
  transition:
    opacity 160ms ease,
    transform 180ms ease;
  pointer-events: none;
}

.swipe-candidate-card__peek--visible {
  opacity: 0.88;
  transform: translate3d(0.82rem, 0.95rem, 0) scale(0.962);
}

.swipe-candidate-card__peek-cover {
  position: relative;
  aspect-ratio: 4 / 6;
  overflow: hidden;
}

.swipe-candidate-card__peek-cover-image,
.swipe-candidate-card__peek-cover-fallback {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.swipe-candidate-card__peek-cover-image {
  object-fit: cover;
  opacity: 0;
  transition: opacity 180ms ease;
}

.swipe-candidate-card__peek-cover-image--ready {
  opacity: 1;
}

.swipe-candidate-card__peek-cover-fallback {
  display: grid;
  place-items: center;
  padding: 1rem;
  color: var(--theme-swipe-cover-fallback-text);
  font-size: 1.05rem;
  font-weight: 700;
  text-align: center;
  background: var(--theme-swipe-cover-fallback-overlay);
}

.swipe-candidate-card__peek-cover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 0.85rem;
  background: var(--theme-swipe-cover-overlay);
}

.swipe-candidate-card__peek-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
}

.swipe-candidate-card__peek-type-pill,
.swipe-candidate-card__peek-next-pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.75rem;
  padding: 0.26rem 0.62rem;
  border-radius: var(--radius-full);
  backdrop-filter: blur(12px);
  font-size: 0.74rem;
  font-weight: 700;
}

.swipe-candidate-card__peek-type-pill {
  border: 1px solid var(--theme-swipe-type-pill-border);
  background: var(--theme-swipe-type-pill-background);
  color: var(--theme-swipe-type-pill-text);
}

.swipe-candidate-card__peek-next-pill {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.9);
}

.swipe-candidate-card__peek-hero {
  display: grid;
  gap: 0.22rem;
}

.swipe-candidate-card__peek-eyebrow,
.swipe-candidate-card__peek-copy {
  margin: 0;
}

.swipe-candidate-card__peek-eyebrow {
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.74rem;
  font-weight: 600;
}

.swipe-candidate-card__peek-label,
.swipe-candidate-card__peek-title,
.swipe-candidate-card__peek-meta {
  margin: 0;
}

.swipe-candidate-card__peek-label,
.swipe-candidate-card__peek-meta {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.swipe-candidate-card__peek-title {
  font-size: 1.35rem;
  max-width: 18ch;
  color: #fff;
  line-height: 1.02;
}

.swipe-candidate-card__peek-meta {
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.86rem;
}

.swipe-candidate-card__intents {
  position: absolute;
  inset: 1rem 1rem auto;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  opacity: 0;
  pointer-events: none;
}

.swipe-candidate-card__intents--visible {
  opacity: 1;
}

.swipe-candidate-card__intent {
  display: inline-flex;
  align-items: center;
  min-height: 2.1rem;
  padding: 0.4rem 0.9rem;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  background: var(--theme-swipe-intent-background);
  color: var(--theme-swipe-intent-text);
  font-size: 0.9rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  transition:
    transform 180ms ease,
    background-color 180ms ease,
    border-color 180ms ease;
}

.swipe-candidate-card__intent--active {
  transform: translateY(-2px);
}

.swipe-candidate-card__intent--skip.swipe-candidate-card__intent--active {
  background: color-mix(in srgb, var(--color-surface) 18%, var(--color-warning));
  border-color: color-mix(in srgb, var(--color-warning) 40%, transparent);
}

.swipe-candidate-card__intent--like.swipe-candidate-card__intent--active {
  background: color-mix(in srgb, var(--color-surface) 14%, var(--color-success));
  border-color: color-mix(in srgb, var(--color-success) 45%, transparent);
}

.swipe-candidate-card {
  position: relative;
  z-index: 1;
  overflow: hidden;
  border-radius: calc(var(--radius-xl) + 2px);
  box-shadow: var(--shadow-swipe-card);
  transition:
    transform 220ms ease,
    opacity 220ms ease,
    box-shadow 220ms ease;
}

.swipe-candidate-card--dragging {
  box-shadow: var(--theme-swipe-drag-shadow);
}

.swipe-candidate-card--locked {
  pointer-events: none;
}

.swipe-candidate-card__swipe-surface {
  display: grid;
  touch-action: pan-y;
}

.swipe-candidate-card__cover {
  position: relative;
  aspect-ratio: 4 / 5.55;
  overflow: hidden;
  background: linear-gradient(135deg, #1f2937, #334155);
}

.swipe-candidate-card__cover--sunrise {
  background: linear-gradient(135deg, #1d3557, #e76f51);
}

.swipe-candidate-card__cover--ocean {
  background: linear-gradient(135deg, #243b53, #2a9d8f);
}

.swipe-candidate-card__cover--mint {
  background: linear-gradient(135deg, #2b2d42, #84a98c);
}

.swipe-candidate-card__cover-image,
.swipe-candidate-card__cover-fallback {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.swipe-candidate-card__cover-image {
  object-fit: cover;
  opacity: 0;
  transition: opacity 180ms ease;
}

.swipe-candidate-card__cover-image--ready {
  opacity: 1;
}

.swipe-candidate-card__cover-fallback {
  display: grid;
  place-items: center;
  padding: 2rem;
  color: var(--theme-swipe-cover-fallback-text);
  font-size: clamp(1.45rem, 4vw, 2rem);
  font-weight: 700;
  text-align: center;
  background: var(--theme-swipe-cover-fallback-overlay);
}

.swipe-candidate-card__cover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 1rem;
  background: var(--theme-swipe-cover-overlay);
}

.swipe-candidate-card__hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.swipe-candidate-card__type-pill {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--theme-swipe-type-pill-border);
  border-radius: var(--radius-full);
  backdrop-filter: blur(12px);
  background: var(--theme-swipe-type-pill-background);
  color: var(--theme-swipe-type-pill-text);
  font-size: 0.84rem;
  font-weight: 700;
}

.swipe-candidate-card__score-badge {
  display: grid;
  justify-items: end;
  gap: 0.1rem;
  min-width: 6.1rem;
  padding: 0.7rem 0.8rem;
  border-radius: 1.1rem;
  backdrop-filter: blur(14px);
  background: var(--theme-swipe-score-background);
  color: #fff;
}

.swipe-candidate-card__score-badge--success {
  background: var(--theme-swipe-score-success-background);
}

.swipe-candidate-card__score-badge--accent {
  background: var(--theme-swipe-score-accent-background);
}

.swipe-candidate-card__score-badge--muted {
  background: var(--theme-swipe-score-background);
}

.swipe-candidate-card__score-label {
  font-size: 1.35rem;
  font-weight: 800;
}

.swipe-candidate-card__score-caption {
  font-size: 0.76rem;
  text-align: right;
  color: var(--theme-swipe-score-caption);
}

.swipe-candidate-card__hero-copy {
  display: grid;
  gap: 0.3rem;
}

.swipe-candidate-card__eyebrow,
.swipe-candidate-card__title,
.swipe-candidate-card__meta,
.swipe-candidate-card__headline,
.swipe-candidate-card__supporting-copy,
.swipe-candidate-card__affordance-copy,
.swipe-candidate-card__details-eyebrow,
.swipe-candidate-card__details-title,
.swipe-candidate-card__details-label,
.swipe-candidate-card__details-copy,
.swipe-candidate-card__details-note,
.swipe-candidate-card__details-source,
.swipe-candidate-card__tag-group-title {
  margin: 0;
}

.swipe-candidate-card__eyebrow {
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.swipe-candidate-card__title {
  color: #fff;
  font-size: clamp(1.75rem, 5vw, 2.5rem);
  line-height: 0.98;
}

.swipe-candidate-card__meta {
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.96rem;
}

.swipe-candidate-card__body {
  display: grid;
  gap: 1rem;
  padding: 1.15rem 1.1rem 1.2rem;
}

.swipe-candidate-card__headline {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.swipe-candidate-card__supporting-copy,
.swipe-candidate-card__affordance-copy,
.swipe-candidate-card__details-note,
.swipe-candidate-card__details-source {
  color: var(--color-text-secondary);
}

.swipe-candidate-card__reason-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.swipe-candidate-card__reason-chip {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.75rem;
  border: 1px solid color-mix(in srgb, var(--color-accent) 14%, var(--color-border));
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--color-accent-soft) 55%, var(--color-surface));
  color: var(--color-text-primary);
  font-size: 0.88rem;
  font-weight: 600;
}

.swipe-candidate-card__affordance {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.9rem;
  padding-top: 0.15rem;
}

.swipe-candidate-card__details-button {
  min-height: 2.8rem;
  padding-inline: 1rem;
  border-radius: var(--radius-full);
  border-color: var(--color-border);
  background: var(--color-surface-secondary);
}

.swipe-candidate-card__details-panel {
  display: grid;
  gap: 1rem;
  padding: 0 1.1rem 1.1rem;
}

.swipe-candidate-card__details-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding-top: 0.1rem;
}

.swipe-candidate-card__details-eyebrow {
  color: var(--color-accent-dark);
  font-size: 0.82rem;
  font-weight: 700;
}

.swipe-candidate-card__details-title {
  font-size: 1.15rem;
}

.swipe-candidate-card__open-link {
  min-height: 2.6rem;
}

.swipe-candidate-card__details-grid {
  display: grid;
  gap: 0.9rem;
  max-height: min(46vh, 25rem);
  overflow: auto;
  padding-right: 0.1rem;
}

.swipe-candidate-card__details-block {
  display: grid;
  gap: 0.55rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface-secondary) 76%, var(--color-surface));
}

.swipe-candidate-card__details-label,
.swipe-candidate-card__tag-group-title {
  font-weight: 700;
  color: var(--color-text-primary);
}

.swipe-candidate-card__details-note--error {
  color: var(--color-error);
}

.swipe-candidate-card__details-source a {
  color: var(--color-accent-dark);
}

.swipe-candidate-card__tag-group {
  display: grid;
  gap: 0.45rem;
}

.swipe-candidate-card__tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.swipe-candidate-card__technical {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface-secondary);
}

.swipe-candidate-card__technical summary {
  cursor: pointer;
  padding: 0.95rem 1rem;
  font-weight: 700;
  list-style: none;
}

.swipe-candidate-card__technical summary::-webkit-details-marker {
  display: none;
}

.swipe-candidate-card__technical-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  padding: 0 1rem 1rem;
}

.swipe-candidate-card__technical-grid div {
  padding: 0.9rem;
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.swipe-candidate-card__technical-grid dt {
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.swipe-candidate-card__technical-grid dd {
  margin: 0.35rem 0 0;
  font-weight: 700;
}

@media (max-width: 720px) {
  .swipe-candidate-card__stage {
    padding-right: 0.55rem;
    padding-bottom: 1.1rem;
  }

  .swipe-candidate-card__peek {
    inset: 0.85rem 0 0.1rem 0.6rem;
    transform: translate3d(0.28rem, 0.4rem, 0) scale(0.986);
  }

  .swipe-candidate-card__peek--visible {
    transform: translate3d(0.55rem, 0.75rem, 0) scale(0.978);
  }

  .swipe-candidate-card__peek-cover-overlay {
    padding: 0.7rem;
  }

  .swipe-candidate-card__peek-title {
    font-size: 1.12rem;
  }

  .swipe-candidate-card__intents {
    inset-inline: 0.75rem;
  }

  .swipe-candidate-card__body,
  .swipe-candidate-card__details-panel {
    padding-inline: 0.95rem;
  }

  .swipe-candidate-card__affordance,
  .swipe-candidate-card__details-header {
    align-items: stretch;
    grid-template-columns: 1fr;
  }

  .swipe-candidate-card__affordance {
    display: grid;
  }

  .swipe-candidate-card__details-grid {
    max-height: none;
    overflow: visible;
  }

  .swipe-candidate-card__technical-grid {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 960px) {
  .swipe-candidate-card__stage {
    padding-right: 0.75rem;
    padding-bottom: 1rem;
  }

  .swipe-candidate-card__peek {
    inset: 0.7rem 0 0 0.8rem;
    transform: translate3d(0.42rem, 0.5rem, 0) scale(0.982);
  }

  .swipe-candidate-card__peek--visible {
    transform: translate3d(0.68rem, 0.82rem, 0) scale(0.968);
  }

  .swipe-candidate-card__cover {
    aspect-ratio: 4 / 5.2;
  }

  .swipe-candidate-card__cover-overlay {
    padding: 0.92rem;
  }

  .swipe-candidate-card__title {
    font-size: clamp(1.7rem, 2.6vw, 2.2rem);
  }

  .swipe-candidate-card__body {
    gap: 0.85rem;
    padding: 1rem 1rem 1.05rem;
  }

  .swipe-candidate-card__details-grid {
    max-height: min(40vh, 22rem);
  }
}

@media (max-width: 520px) {
  .swipe-candidate-card__cover {
    aspect-ratio: 7 / 9;
  }

  .swipe-candidate-card__cover-overlay {
    padding: 0.85rem;
  }

  .swipe-candidate-card__hero-top {
    align-items: flex-start;
  }

  .swipe-candidate-card__score-badge {
    min-width: 5.5rem;
    padding-inline: 0.7rem;
  }

  .swipe-candidate-card__score-label {
    font-size: 1.18rem;
  }
}
</style>
