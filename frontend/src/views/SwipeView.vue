<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { listCandidates } from '@/api/candidates';
import { getMatches } from '@/api/matches';
import AppMessage from '@/components/common/AppMessage.vue';
import SwipeCandidateCard from '@/components/swipe/SwipeCandidateCard.vue';
import SwipeDecisionControls from '@/components/swipe/SwipeDecisionControls.vue';
import SwipeProgress from '@/components/swipe/SwipeProgress.vue';
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

const currentItem = computed(() => queue.value[0] ?? null);
const nextItem = computed(() => queue.value[1] ?? null);
const remainingCount = computed(() => queue.value.length);
const hasQueue = computed(() => totalCount.value > 0);
const interactionLocked = computed(() => decisionPending.value);
const matchInsightsAvailable = computed(() => matching.value !== null);
const profileReady = computed(() => matching.value?.interestProfile.isReadyForMatching ?? null);
const scoresSuppressed = computed(() => matching.value?.scoresSuppressed ?? false);
const profileStatusMessage = computed(
  () =>
    matching.value?.interestProfile.explanationMessage
    ?? matching.value?.explanationMessage
    ?? 'Ein paar weitere starke Bewertungen helfen MoodMatch beim Einordnen.',
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
    liveMessage.value = `${queue.value[0].candidate.media.title} ist jetzt aktiv.`;
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
    ? `${title} wurde geliket. ${nextTitle} ist jetzt aktiv.`
    : `${title} wurde geliket.`;
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
    ? `${title} wurde fuer spaeter zur Seite gelegt. ${nextTitle} ist jetzt aktiv.`
    : `${title} wurde fuer spaeter zur Seite gelegt.`;
  decisionPending.value = false;
}

function handleDetailsToggle() {
  if (!currentItem.value || interactionLocked.value) {
    return;
  }

  actionErrorMessage.value = '';
  detailsExpanded.value = !detailsExpanded.value;
  liveMessage.value = detailsExpanded.value
    ? `Mehr Details zu ${currentItem.value.candidate.media.title} wurden geoeffnet.`
    : `Mehr Details zu ${currentItem.value.candidate.media.title} wurden geschlossen.`;
}

function handleDetailsLoadError(message: string) {
  actionErrorMessage.value = message;
  liveMessage.value = `Details fuer ${currentItem.value?.candidate.media.title ?? 'den Titel'} konnten nicht geladen werden.`;
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

  return 'Die Empfehlungen konnten nicht geladen werden.';
}

function toMatchesErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return `${error.message} Du kannst trotzdem weiter durch die Titel swipen.`;
  }

  return 'Die Match-Hinweise konnten nicht geladen werden. Du kannst trotzdem weiter durch die Titel swipen.';
}
</script>

<template>
  <section class="swipe-view">
    <header class="swipe-view__header">
      <div class="swipe-view__header-copy">
        <p class="swipe-view__eyebrow">
          Swipe Recommendations
        </p>
        <h1 class="swipe-view__title">
          Deine naechste Empfehlung
        </h1>
        <p class="swipe-view__copy">
          Schnell liken, nach links abwinken oder Details nur bei Bedarf aufklappen.
          MoodMatch bleibt bei derselben Datenbasis, fuehlt sich hier aber deutlich mehr nach
          einer mobilen Discovery-Ansicht an.
        </p>
      </div>

      <div class="swipe-view__actions">
        <RouterLink
          :to="{ name: 'candidates' }"
          class="button swipe-view__action-button"
        >
          Kandidaten
        </RouterLink>
        <RouterLink
          :to="{ name: 'external-search' }"
          class="button swipe-view__action-button swipe-view__action-button--ghost"
        >
          Importieren
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
      title="Empfehlungen werden vorbereitet"
      description="Kandidaten, Match-Hinweise und die Swipe-Reihenfolge werden geladen."
      tone="info"
    />

    <AppMessage
      v-else-if="fatalErrorMessage"
      title="Swipe-Empfehlungen konnten nicht geladen werden"
      :description="fatalErrorMessage"
      tone="error"
    >
      <div class="swipe-view__message-actions">
        <button
          class="button swipe-view__message-button"
          type="button"
          @click="loadQueue"
        >
          Erneut versuchen
        </button>
      </div>
    </AppMessage>

    <template v-else>
      <AppMessage
        v-if="matchWarningMessage"
        title="Match-Hinweise fehlen gerade"
        :description="matchWarningMessage"
        tone="warning"
      />

      <AppMessage
        v-if="actionErrorMessage"
        title="Aktion fehlgeschlagen"
        :description="actionErrorMessage"
        tone="error"
      />

      <AppMessage
        v-if="showProfileWarmupBanner"
        title="Dein Profil lernt noch"
        :description="profileStatusMessage"
        tone="warning"
      >
        <div class="swipe-view__message-actions">
          <RouterLink
            :to="{ name: 'media-list' }"
            class="button swipe-view__message-button"
          >
            Mehr Medien bewerten
          </RouterLink>
          <RouterLink
            :to="{ name: 'external-search' }"
            class="button swipe-view__message-button swipe-view__message-button--ghost"
          >
            Titel importieren
          </RouterLink>
        </div>
      </AppMessage>

      <template v-if="showProfileWarmupEmptyState">
        <AppMessage
          title="Profil noch nicht bereit"
          :description="profileStatusMessage"
          tone="warning"
        >
          <div class="swipe-view__message-actions">
            <RouterLink
              :to="{ name: 'media-list' }"
              class="button swipe-view__message-button"
            >
              Medien bewerten
            </RouterLink>
            <RouterLink
              :to="{ name: 'external-search' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              Importieren
            </RouterLink>
          </div>
        </AppMessage>
      </template>

      <template v-else-if="showNoRecommendationsState">
        <AppMessage
          title="Noch keine Empfehlungen"
          description="Fuelle deine WANT_TO_CONSUME Liste, importiere weitere Titel oder pruefe bestehende Kandidaten, damit hier neue Karten auftauchen."
        >
          <div class="swipe-view__message-actions">
            <RouterLink
              :to="{ name: 'media-create' }"
              class="button swipe-view__message-button"
            >
              Titel anlegen
            </RouterLink>
            <RouterLink
              :to="{ name: 'external-search' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              Extern importieren
            </RouterLink>
            <RouterLink
              :to="{ name: 'candidates' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              Kandidaten pruefen
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
          title="All caught up"
          :description="`Du hast ${stats.liked} Titel geliket und ${stats.skipped} nach links aussortiert. Diese Entscheidungen bleiben in dieser Runde lokal.`"
          tone="info"
        >
          <div class="swipe-view__message-actions">
            <button
              class="button swipe-view__message-button"
              type="button"
              @click="loadQueue"
            >
              Runde neu laden
            </button>
            <RouterLink
              :to="{ name: 'external-search' }"
              class="button swipe-view__message-button swipe-view__message-button--ghost"
            >
              Neue Titel holen
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
      Likes und linke Swipes bleiben in dieser Runde lokal. Fuer dauerhafte Aenderungen kannst du die
      Detailansicht oder die Kandidatenliste nutzen.
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
  background:
    radial-gradient(circle at top center, rgba(124, 92, 252, 0.26), transparent 34%),
    radial-gradient(circle at bottom center, rgba(236, 72, 153, 0.18), transparent 36%),
    linear-gradient(180deg, #0f1630 0%, #0a1024 100%);
  color: #f8f7ff;
  box-shadow: 0 28px 60px rgba(8, 15, 32, 0.28);
}

.swipe-view::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.02), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 28%);
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
  color: #ffb4b8;
  font-size: 0.86rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.swipe-view__title {
  font-size: clamp(2rem, 5vw, 3rem);
  line-height: 0.98;
}

.swipe-view__copy,
.swipe-view__footnote {
  color: rgba(236, 239, 255, 0.78);
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
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  backdrop-filter: blur(14px);
}

.swipe-view__action-button--ghost,
.swipe-view__message-button--ghost {
  background: rgba(255, 255, 255, 0.04);
}

.swipe-view__deck {
  display: grid;
  place-items: center;
}

.swipe-view__card-column {
  width: min(100%, 30rem);
  display: grid;
  gap: 1rem;
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
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(11, 17, 36, 0.72);
  box-shadow: none;
  backdrop-filter: blur(18px);
}

.swipe-view :deep(.app-message__title) {
  color: #fff;
}

.swipe-view :deep(.app-message__description) {
  color: rgba(236, 239, 255, 0.78);
}

.swipe-view :deep(.app-message--warning) {
  background: rgba(68, 42, 9, 0.74);
}

.swipe-view :deep(.app-message--error) {
  background: rgba(74, 20, 33, 0.78);
}

.swipe-view :deep(.app-message--info) {
  background: rgba(17, 31, 67, 0.76);
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
