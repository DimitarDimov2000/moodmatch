<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';

import MatchScoreDisplay from '@/components/matching/MatchScoreDisplay.vue';
import {
  commitmentLevelLabels,
  formatDecimal,
  mediaTypeLabels,
} from '@/components/matching/matching-format';
import TagChip from '@/components/tags/TagChip.vue';
import type {
  SwipeDecisionAction,
  SwipeGestureIntent,
  SwipeQueueItem,
} from '@/types/swipe';

const HORIZONTAL_THRESHOLD = 120;
const VERTICAL_THRESHOLD = 110;
const HORIZONTAL_CLAMP = 150;
const VERTICAL_CLAMP = 140;
const EXIT_DISTANCE_X = 220;
const EXIT_DISTANCE_Y = 180;
const PREVIEW_DRAG_THRESHOLD = 30;

const props = withDefaults(
  defineProps<{
    item: SwipeQueueItem;
    matchInsightsAvailable: boolean;
    interactionLocked?: boolean;
  }>(),
  {
    interactionLocked: false,
  },
);

const emit = defineEmits<{
  decisionRequest: [action: SwipeDecisionAction];
}>();

const readinessLabel = computed(() =>
  props.item.candidate.isCompleteForMatching ? 'Matching bereit' : 'Noch nicht matching-bereit',
);
const readinessToneClass = computed(() =>
  props.item.candidate.isCompleteForMatching
    ? 'swipe-candidate-card__state--ready'
    : 'swipe-candidate-card__state--warning',
);
const scoreStateLabel = computed(() => {
  if (!props.matchInsightsAvailable) {
    return 'Match-Hinweise derzeit nicht verfuegbar';
  }

  if (!props.item.match) {
    return 'Noch kein Match-Ergebnis vorhanden';
  }

  if (props.item.match.relativeScore === null) {
    return 'Score vorhanden, aber bewusst ohne Prozent';
  }

  return 'Relativer Match-Score verfuegbar';
});
const scoreDetail = computed(() => {
  if (!props.matchInsightsAvailable) {
    return 'Du kannst trotzdem lokal durch die Kandidaten gehen.';
  }

  if (!props.item.match) {
    return 'Sobald ein Match vorliegt, erscheint hier die kurze Einordnung.';
  }

  return props.item.match.explanationMessage;
});
const matchingTagCountLabel = computed(() => props.item.match?.matchingTagCount ?? 0);
const candidateTagCountLabel = computed(
  () => props.item.match?.candidateTagCount ?? props.item.candidate.media.tags.length,
);
const adjustedScoreLabel = computed(() => formatDecimal(props.item.match?.adjustedScore ?? null));
const rawScoreLabel = computed(() => formatDecimal(props.item.match?.rawScore ?? null));

const prefersReducedMotion = ref(false);
const previewExpanded = ref(false);
const dragging = ref(false);
const awaitingDecision = ref(false);
const exiting = ref(false);
const dragX = ref(0);
const dragY = ref(0);
const startX = ref(0);
const startY = ref(0);
const activePointerId = ref<number | null>(null);

onMounted(() => {
  prefersReducedMotion.value =
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
});

watch(
  () => props.item.candidate.media.id,
  () => {
    previewExpanded.value = false;
    resetGesturePosition();
  },
);

const gestureIntent = computed<SwipeGestureIntent>(() => {
  if (exiting.value) {
    if (dragX.value > 0) {
      return 'like';
    }

    if (dragX.value < 0) {
      return 'reject';
    }

    if (dragY.value > 0) {
      return 'skip';
    }

    return 'none';
  }

  return getGestureIntent(dragX.value, dragY.value);
});
const showIntentIndicators = computed(
  () => dragging.value || awaitingDecision.value || exiting.value,
);
const cardTransitionDuration = computed(() => {
  if (dragging.value) {
    return '0ms';
  }

  return prefersReducedMotion.value ? '40ms' : '180ms';
});
const cardRotation = computed(() => {
  if (isVerticalIntent(gestureIntent.value)) {
    return 0;
  }

  return clamp(dragX.value / 24, -6, 6);
});
const cardStyle = computed(() => ({
  transform: `translate3d(${dragX.value}px, ${dragY.value}px, 0) rotate(${cardRotation.value}deg)`,
  transitionDuration: cardTransitionDuration.value,
}));
const hasMatchDetails = computed(
  () =>
    Boolean(props.item.match) &&
    (props.item.match?.matchingTags.length ?? 0) + (props.item.match?.extraCandidateTags.length ?? 0) > 0,
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
  const horizontalDominant = Math.abs(rawX) >= Math.abs(rawY);

  dragX.value = clamp(rawX, -HORIZONTAL_CLAMP, HORIZONTAL_CLAMP);
  dragY.value = clamp(
    horizontalDominant ? rawY * 0.35 : rawY,
    -VERTICAL_CLAMP,
    VERTICAL_CLAMP,
  );
}

function handlePointerUp(event: PointerEvent) {
  if (!dragging.value || event.pointerId !== activePointerId.value) {
    return;
  }

  releasePointer(event);

  const nextIntent = getGestureIntent(dragX.value, dragY.value);

  if (nextIntent === 'like' && Math.abs(dragX.value) >= HORIZONTAL_THRESHOLD) {
    awaitDecision('like');
    return;
  }

  if (nextIntent === 'reject' && Math.abs(dragX.value) >= HORIZONTAL_THRESHOLD) {
    awaitDecision('reject');
    return;
  }

  if (nextIntent === 'skip' && dragY.value >= VERTICAL_THRESHOLD) {
    awaitDecision('skip');
    return;
  }

  if (nextIntent === 'preview' && Math.abs(dragY.value) >= VERTICAL_THRESHOLD) {
    previewExpanded.value = true;
    resetGesturePosition();
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

  if (action === 'like') {
    dragX.value = EXIT_DISTANCE_X;
    dragY.value = 0;
  } else if (action === 'reject') {
    dragX.value = -EXIT_DISTANCE_X;
    dragY.value = 0;
  } else {
    dragX.value = 0;
    dragY.value = EXIT_DISTANCE_Y;
  }

  await wait(prefersReducedMotion.value ? 40 : 180);
}

function resetGesturePosition() {
  dragging.value = false;
  awaitingDecision.value = false;
  exiting.value = false;
  activePointerId.value = null;
  dragX.value = 0;
  dragY.value = 0;
}

function closePreview() {
  previewExpanded.value = false;
}

function getGestureIntent(x: number, y: number): SwipeGestureIntent {
  const absoluteX = Math.abs(x);
  const absoluteY = Math.abs(y);

  if (absoluteX < PREVIEW_DRAG_THRESHOLD && absoluteY < PREVIEW_DRAG_THRESHOLD) {
    return 'none';
  }

  if (absoluteX >= absoluteY) {
    return x >= 0 ? 'like' : 'reject';
  }

  return y >= 0 ? 'skip' : 'preview';
}

function isVerticalIntent(intent: SwipeGestureIntent): boolean {
  return intent === 'skip' || intent === 'preview';
}

function isInteractiveTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }

  return Boolean(target.closest('button, a, input, select, textarea, [contenteditable="true"]'));
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
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
    <div
      class="swipe-candidate-card__intents"
      :class="{ 'swipe-candidate-card__intents--visible': showIntentIndicators }"
      aria-hidden="true"
    >
      <span
        class="swipe-candidate-card__intent swipe-candidate-card__intent--preview"
        :class="{ 'swipe-candidate-card__intent--active': gestureIntent === 'preview' }"
      >
        ↑ Vorschau
      </span>
      <span
        class="swipe-candidate-card__intent swipe-candidate-card__intent--reject"
        :class="{ 'swipe-candidate-card__intent--active': gestureIntent === 'reject' }"
      >
        ← Ablehnen
      </span>
      <span
        class="swipe-candidate-card__intent swipe-candidate-card__intent--like"
        :class="{ 'swipe-candidate-card__intent--active': gestureIntent === 'like' }"
      >
        → Liken
      </span>
      <span
        class="swipe-candidate-card__intent swipe-candidate-card__intent--skip"
        :class="{ 'swipe-candidate-card__intent--active': gestureIntent === 'skip' }"
      >
        ↓ Ueberspringen
      </span>
    </div>

    <article
      class="swipe-candidate-card page-card"
      :class="{
        'swipe-candidate-card--dragging': dragging,
        'swipe-candidate-card--locked': interactionLocked || awaitingDecision,
      }"
      :style="cardStyle"
      @pointerdown="handlePointerDown"
      @pointermove="handlePointerMove"
      @pointerup="handlePointerUp"
      @pointercancel="handlePointerCancel"
    >
      <div class="swipe-candidate-card__hero">
        <div class="swipe-candidate-card__copy">
          <p class="eyebrow">
            Aktuelle Karte
          </p>
          <h2 class="swipe-candidate-card__title">
            {{ item.candidate.media.title }}
          </h2>
          <p class="swipe-candidate-card__meta">
            {{ mediaTypeLabels[item.candidate.media.mediaType] }} ·
            {{ commitmentLevelLabels[item.candidate.media.commitmentLevel] }}
            <span v-if="item.candidate.media.releaseYear">
              · {{ item.candidate.media.releaseYear }}
            </span>
          </p>

          <div class="swipe-candidate-card__states">
            <span
              class="swipe-candidate-card__state"
              :class="readinessToneClass"
            >
              {{ readinessLabel }}
            </span>
            <span class="swipe-candidate-card__state swipe-candidate-card__state--neutral">
              {{ scoreStateLabel }}
            </span>
            <span
              v-if="previewExpanded"
              class="swipe-candidate-card__state swipe-candidate-card__state--preview"
            >
              Vorschau offen
            </span>
          </div>
        </div>

        <MatchScoreDisplay
          :score="item.match?.relativeScore ?? null"
          :suppressed="!matchInsightsAvailable"
          insufficient-label="Keine Prozentangabe verfuegbar."
        />
      </div>

      <div class="swipe-candidate-card__grid">
        <section class="swipe-candidate-card__panel">
          <p class="swipe-candidate-card__label">
            Erwartete Tags
          </p>
          <p
            v-if="item.candidate.media.tags.length === 0"
            class="swipe-candidate-card__empty"
          >
            Fuer diesen Kandidaten wurden noch keine erwarteten Tags hinterlegt.
          </p>
          <div
            v-else
            class="swipe-candidate-card__tags"
          >
            <TagChip
              v-for="tag in item.candidate.media.tags"
              :key="tag.id"
              :tag="tag"
            />
          </div>
          <p class="swipe-candidate-card__hint">
            Kandidaten-Tags basieren auf deiner Einschaetzung.
          </p>
        </section>

        <section class="swipe-candidate-card__panel swipe-candidate-card__panel--accent">
          <p class="swipe-candidate-card__label">
            Match-Einordnung
          </p>
          <p class="swipe-candidate-card__detail">
            {{ scoreDetail }}
          </p>
          <p
            v-if="item.match"
            class="swipe-candidate-card__hint"
          >
            {{ item.match.candidateTagsNote }}
          </p>
        </section>
      </div>

      <section
        v-if="previewExpanded"
        class="swipe-candidate-card__preview"
      >
        <div class="swipe-candidate-card__preview-header">
          <div>
            <p class="swipe-candidate-card__label">
              Details-Vorschau
            </p>
            <p class="swipe-candidate-card__hint">
              Fuer die Vollansicht bleiben der Details-Button und die Enter-Taste erhalten.
            </p>
          </div>

          <button
            class="button button--secondary swipe-candidate-card__preview-close"
            type="button"
            @click="closePreview"
          >
            Vorschau schliessen
          </button>
        </div>

        <dl class="swipe-candidate-card__metrics">
          <div>
            <dt>Matching-Tags</dt>
            <dd>{{ matchingTagCountLabel }} / {{ candidateTagCountLabel }}</dd>
          </div>
          <div>
            <dt>Adjusted Score</dt>
            <dd>{{ adjustedScoreLabel ?? 'Nicht verfuegbar' }}</dd>
          </div>
          <div>
            <dt>Raw Score</dt>
            <dd>{{ rawScoreLabel ?? 'Nicht verfuegbar' }}</dd>
          </div>
        </dl>

        <div class="swipe-candidate-card__preview-grid">
          <section class="swipe-candidate-card__panel">
            <p class="swipe-candidate-card__label">
              Kurz erklaert
            </p>
            <p class="swipe-candidate-card__detail">
              {{ scoreDetail }}
            </p>
          </section>

          <section class="swipe-candidate-card__panel">
            <p class="swipe-candidate-card__label">
              Ueberschneidungen
            </p>
            <p
              v-if="!hasMatchDetails"
              class="swipe-candidate-card__empty"
            >
              Noch keine weiterfuehrenden Match-Details verfuegbar.
            </p>
            <div
              v-if="item.match?.matchingTags.length"
              class="swipe-candidate-card__preview-section"
            >
              <p class="swipe-candidate-card__sub-label">
                Profiltreffer
              </p>
              <div class="swipe-candidate-card__tags">
                <TagChip
                  v-for="entry in item.match.matchingTags"
                  :key="entry.tag.id"
                  :tag="entry.tag"
                />
              </div>
            </div>
            <div
              v-if="item.match?.extraCandidateTags.length"
              class="swipe-candidate-card__preview-section"
            >
              <p class="swipe-candidate-card__sub-label">
                Erwartete Tags ohne Treffer
              </p>
              <div class="swipe-candidate-card__tags">
                <TagChip
                  v-for="tag in item.match.extraCandidateTags"
                  :key="tag.id"
                  :tag="tag"
                />
              </div>
            </div>
          </section>
        </div>
      </section>
    </article>
  </div>
</template>

<style scoped>
.swipe-candidate-card__stage {
  position: relative;
  display: grid;
}

.swipe-candidate-card__intents {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 0.85rem;
  opacity: 0.72;
  transition: opacity 160ms ease;
}

.swipe-candidate-card__intents--visible {
  opacity: 1;
}

.swipe-candidate-card__intent {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.25rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-surface) 84%, transparent);
  color: var(--color-text-secondary);
  font-size: 0.88rem;
  font-weight: 600;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    transform 160ms ease;
}

.swipe-candidate-card__intent--active {
  transform: translateY(-1px);
}

.swipe-candidate-card__intent--like.swipe-candidate-card__intent--active {
  background: var(--color-success-soft);
  border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  color: var(--color-success);
}

.swipe-candidate-card__intent--reject.swipe-candidate-card__intent--active {
  background: var(--color-error-soft);
  border-color: color-mix(in srgb, var(--color-error) 30%, var(--color-border));
  color: var(--color-error);
}

.swipe-candidate-card__intent--skip.swipe-candidate-card__intent--active {
  background: var(--color-warning-soft);
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
  color: var(--color-warning);
}

.swipe-candidate-card__intent--preview.swipe-candidate-card__intent--active {
  background: var(--color-info-soft);
  border-color: color-mix(in srgb, var(--color-info) 30%, var(--color-border));
  color: var(--color-info);
}

.swipe-candidate-card {
  display: grid;
  gap: 1.5rem;
  padding: 1.5rem;
  background:
    radial-gradient(
      circle at top left,
      color-mix(in srgb, var(--color-accent-soft) 78%, transparent),
      transparent 42%
    ),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--color-surface-secondary) 78%, var(--color-surface)),
      var(--color-surface)
    );
  box-shadow: var(--shadow-swipe-card);
  transition-property: transform, box-shadow;
  transition-timing-function: ease;
  will-change: transform;
  touch-action: none;
  user-select: none;
}

.swipe-candidate-card--dragging {
  box-shadow: 0 22px 52px rgba(15, 23, 42, 0.18);
}

.swipe-candidate-card--locked {
  cursor: default;
}

.swipe-candidate-card__hero {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.42fr);
  align-items: start;
}

.swipe-candidate-card__copy {
  display: grid;
  gap: 0.65rem;
}

.swipe-candidate-card__title,
.swipe-candidate-card__meta,
.swipe-candidate-card__label,
.swipe-candidate-card__detail,
.swipe-candidate-card__hint,
.swipe-candidate-card__empty {
  margin: 0;
}

.swipe-candidate-card__title {
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 0.98;
}

.swipe-candidate-card__meta,
.swipe-candidate-card__hint,
.swipe-candidate-card__empty {
  color: var(--color-text-secondary);
}

.swipe-candidate-card__states {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.swipe-candidate-card__state {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 0.92rem;
  font-weight: 600;
}

.swipe-candidate-card__state--ready {
  background: var(--color-success-soft);
  border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  color: var(--color-success);
}

.swipe-candidate-card__state--warning {
  background: var(--color-warning-soft);
  border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border));
  color: var(--color-warning);
}

.swipe-candidate-card__state--neutral {
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
}

.swipe-candidate-card__state--preview {
  background: var(--color-info-soft);
  border-color: color-mix(in srgb, var(--color-info) 28%, var(--color-border));
  color: var(--color-info);
}

.swipe-candidate-card__grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.swipe-candidate-card__panel {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface) 70%, transparent);
}

.swipe-candidate-card__panel--accent {
  background: color-mix(in srgb, var(--color-info-soft) 42%, var(--color-surface));
}

.swipe-candidate-card__label {
  font-size: 0.9rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--color-text-secondary);
}

.swipe-candidate-card__detail {
  font-size: 1rem;
}

.swipe-candidate-card__preview {
  display: grid;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface-secondary) 90%, var(--color-surface));
}

.swipe-candidate-card__preview-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.swipe-candidate-card__preview-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.swipe-candidate-card__preview-section {
  display: grid;
  gap: 0.55rem;
}

.swipe-candidate-card__sub-label {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-secondary);
}

.swipe-candidate-card__metrics {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
}

.swipe-candidate-card__metrics div {
  padding: 0.85rem 0.95rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.swipe-candidate-card__metrics dt {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.swipe-candidate-card__metrics dd {
  margin: 0.35rem 0 0;
  font-weight: 700;
}

.swipe-candidate-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

@media (max-width: 900px) {
  .swipe-candidate-card__hero,
  .swipe-candidate-card__grid,
  .swipe-candidate-card__preview-grid {
    grid-template-columns: 1fr;
  }

  .swipe-candidate-card__metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .swipe-candidate-card__intents {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .swipe-candidate-card,
  .swipe-candidate-card__intent {
    transition-duration: 0ms !important;
  }
}
</style>
