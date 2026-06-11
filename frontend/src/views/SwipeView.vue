<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { listCandidates } from '@/api/candidates';
import { getMatches } from '@/api/matches';
import { updateMediaStatus } from '@/api/media';
import AppMessage from '@/components/common/AppMessage.vue';
import SwipeCandidateCard from '@/components/swipe/SwipeCandidateCard.vue';
import SwipeDecisionControls from '@/components/swipe/SwipeDecisionControls.vue';
import SwipeProgress from '@/components/swipe/SwipeProgress.vue';
import type { MatchResultResponse, UpdateMediaConsumptionStatusRequest } from '@/types/api';
import type { SwipeDecisionAction, SwipeQueueItem, SwipeQueueStats } from '@/types/swipe';

interface SwipeCandidateCardHandle {
  playDecisionAnimation: (action: SwipeDecisionAction) => Promise<void>;
  resetGesturePosition: () => void;
}

const router = useRouter();
const activeCardRef = ref<SwipeCandidateCardHandle | null>(null);
const queue = ref<SwipeQueueItem[]>([]);
const totalCount = ref(0);
const loading = ref(true);
const fatalErrorMessage = ref('');
const matchWarningMessage = ref('');
const actionErrorMessage = ref('');
const pendingReject = ref(false);
const decisionPending = ref(false);
const matchInsightsAvailable = ref(false);
const liveMessage = ref('');
const stats = reactive<SwipeQueueStats>({
  liked: 0,
  rejected: 0,
  skipped: 0,
});

const currentItem = computed(() => queue.value[0] ?? null);
const remainingCount = computed(() => queue.value.length);
const hasQueue = computed(() => totalCount.value > 0);
const interactionLocked = computed(() => pendingReject.value || decisionPending.value);
const showDoneState = computed(
  () => !loading.value && !fatalErrorMessage.value && hasQueue.value && remainingCount.value === 0,
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
  pendingReject.value = false;
  decisionPending.value = false;
  matchInsightsAvailable.value = false;
  liveMessage.value = '';
  stats.liked = 0;
  stats.rejected = 0;
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
    matchInsightsAvailable.value = true;
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
    ? `${title} wurde lokal geliket. ${nextTitle} ist jetzt aktiv.`
    : `${title} wurde lokal geliket.`;
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
    ? `${title} wurde fuer spaeter uebersprungen. ${nextTitle} ist jetzt aktiv.`
    : `${title} wurde fuer spaeter uebersprungen.`;
  decisionPending.value = false;
}

async function handleReject() {
  if (!currentItem.value || interactionLocked.value) {
    return;
  }

  pendingReject.value = true;
  actionErrorMessage.value = '';
  const title = currentItem.value.candidate.media.title;

  const request: UpdateMediaConsumptionStatusRequest = {
    consumptionStatus: 'NOT_INTERESTED',
    rating: null,
    isFavourite: false,
    confirmDestructiveChange: false,
  };

  try {
    await updateMediaStatus(currentItem.value.candidate.media.id, request);
    await activeCardRef.value?.playDecisionAnimation('reject');
    stats.rejected += 1;
    const nextTitle = removeCurrentItem();
    liveMessage.value = nextTitle
      ? `${title} wurde auf Kein Interesse gesetzt. ${nextTitle} ist jetzt aktiv.`
      : `${title} wurde auf Kein Interesse gesetzt.`;
  } catch (error) {
    actionErrorMessage.value = toRejectErrorMessage(error);
    liveMessage.value = `Ablehnen von ${title} ist fehlgeschlagen.`;
    activeCardRef.value?.resetGesturePosition();
  } finally {
    pendingReject.value = false;
  }
}

async function openDetails() {
  if (!currentItem.value || interactionLocked.value) {
    return;
  }

  actionErrorMessage.value = '';
  await router.push({
    name: 'media-detail',
    params: {
      id: currentItem.value.candidate.media.id,
    },
  });
}

function removeCurrentItem(): string | null {
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
    void handleReject();
    return;
  }

  if (event.key === 'ArrowDown' || event.key.toLowerCase() === 's') {
    event.preventDefault();
    void handleSkip();
    return;
  }

  if (event.key === 'Enter') {
    event.preventDefault();
    void openDetails();
  }
}

function handleGestureDecision(action: SwipeDecisionAction) {
  if (action === 'like') {
    void handleLike();
    return;
  }

  if (action === 'reject') {
    void handleReject();
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

  return 'Die Kandidaten konnten nicht geladen werden.';
}

function toMatchesErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return `${error.message} Die Swipe-Runde bleibt trotzdem nutzbar.`;
  }

  return 'Match-Hinweise konnten nicht geladen werden. Die Swipe-Runde bleibt trotzdem nutzbar.';
}

function toRejectErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }

  return 'Der Kandidat konnte nicht auf Kein Interesse gesetzt werden.';
}
</script>

<template>
  <section class="swipe-view page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Swipe-Modus
        </p>
        <h1 class="page-title">
          Lokale Entscheidungen fuer bestehende Kandidaten
        </h1>
        <p class="page-copy">
          Gehe WANT_TO_CONSUME Kandidaten nacheinander durch. Liken und Ueberspringen
          bleiben lokal in dieser Runde, waehrend Ablehnen den Status auf
          Kein Interesse setzt.
        </p>
      </div>

      <div class="page-actions">
        <RouterLink
          :to="{ name: 'candidates' }"
          class="button button--secondary"
        >
          Zur Kandidatenliste
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
      title="Swipe-Runde wird geladen"
      description="Kandidaten und vorhandene Match-Hinweise werden vorbereitet."
      tone="info"
    />

    <AppMessage
      v-else-if="fatalErrorMessage"
      title="Swipe-Runde konnte nicht geladen werden"
      :description="fatalErrorMessage"
      tone="error"
    >
      <div class="swipe-view__message-actions">
        <button
          class="button button--secondary"
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
        title="Match-Hinweise derzeit nicht verfuegbar"
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
        v-if="!hasQueue"
        title="Noch keine Medienvorschlaege vorhanden"
        description="Lege zuerst Kandidaten mit WANT_TO_CONSUME Status an, damit du sie hier lokal durchgehen kannst."
      >
        <div class="swipe-view__message-actions">
          <RouterLink
            :to="{ name: 'media-create' }"
            class="button button--primary"
          >
            Ersten Kandidaten anlegen
          </RouterLink>
        </div>
      </AppMessage>

      <template v-else>
        <SwipeProgress
          :total-count="totalCount"
          :remaining-count="remainingCount"
          :stats="stats"
        />

        <AppMessage
          v-if="showDoneState"
          title="Runde abgeschlossen"
          :description="`Du hast ${stats.liked} Kandidaten lokal geliket, ${stats.rejected} abgelehnt und ${stats.skipped} uebersprungen. Lokale Likes werden nicht gespeichert.`"
          tone="info"
        >
          <div class="swipe-view__message-actions">
            <button
              class="button button--primary"
              type="button"
              @click="loadQueue"
            >
              Runde neu laden
            </button>
            <RouterLink
              :to="{ name: 'candidates' }"
              class="button button--secondary"
            >
              Kandidaten pruefen
            </RouterLink>
          </div>
        </AppMessage>

        <template v-else-if="currentItem">
          <section class="swipe-view__deck">
            <SwipeCandidateCard
              ref="activeCardRef"
              :item="currentItem"
              :match-insights-available="matchInsightsAvailable"
              :interaction-locked="interactionLocked"
              @decision-request="handleGestureDecision"
            />

            <SwipeDecisionControls
              :pending="interactionLocked"
              :reject-persists="true"
              @like="handleLike"
              @reject="handleReject"
              @skip="handleSkip"
              @details="openDetails"
            />
          </section>
        </template>
      </template>
    </template>
  </section>
</template>

<style scoped>
.swipe-view__deck {
  display: grid;
  gap: 1rem;
}

.swipe-view__message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
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
</style>
