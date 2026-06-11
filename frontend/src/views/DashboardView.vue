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
import type {
  CandidateSelectionResponse,
  InterestProfileResponse,
  MatchResultResponse,
  MatchingResponse,
  MediaResponse,
} from "@/types/api";

const media = ref<MediaResponse[] | null>(null);
const profile = ref<InterestProfileResponse | null>(null);
const candidateSelection = ref<CandidateSelectionResponse | null>(null);
const matching = ref<MatchingResponse | null>(null);
const loading = ref(true);
const loadWarnings = ref<string[]>([]);

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
const bestMatch = computed<MatchResultResponse | null>(
  () => matching.value?.matches[0] ?? null,
);

const heroTitle = computed(() => {
  if (
    (media.value?.length ?? 0) === 0 &&
    (candidateSelection.value?.candidates.length ?? 0) === 0
  ) {
    return "Lege zuerst konsumierte Medien oder Kandidaten an.";
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return "Dein Profil braucht noch mehr bewertete Medien.";
  }

  if (matching.value?.scoresSuppressed) {
    return "Matches sind da, aber noch nicht sauber vergleichbar.";
  }

  if (bestMatch.value?.relativeScore !== null) {
    return `${bestMatch.value?.candidate.media.title} fuehrt aktuell deine Matches an.`;
  }

  return "Dein lokales MoodMatch-Dashboard zeigt den Stand von Profil, Kandidaten und Matches.";
});

const heroCopy = computed(() => {
  if (
    (media.value?.length ?? 0) === 0 &&
    (candidateSelection.value?.candidates.length ?? 0) === 0
  ) {
    return "Sobald du konsumierte Medien oder Kandidaten anlegst, erscheinen hier direkte Einstiege in Profil, Match-Vergleich und Medienpflege.";
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return profile.value.explanationMessage;
  }

  if (matching.value?.scoresSuppressed) {
    return matching.value.explanationMessage;
  }

  if (bestMatch.value?.relativeScore !== null) {
    return "Die Prozentangabe bleibt bewusst relativ und erscheint nur, wenn das Backend genug vergleichbare Kandidaten erkennt.";
  }

  return "Alle vier Bereiche werden ausschliesslich aus bestehenden Frontend-API-Aufrufen zusammengesetzt, ohne eigenes Dashboard-Backend.";
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
      `Medien: ${toUserMessage(mediaResult.reason, "Die Medien konnten nicht geladen werden.")}`,
    );
  }

  if (profileResult.status === "fulfilled") {
    profile.value = profileResult.value;
  } else {
    profile.value = null;
    loadWarnings.value.push(
      `Profil: ${toUserMessage(profileResult.reason, "Das Profil konnte nicht geladen werden.")}`,
    );
  }

  if (candidatesResult.status === "fulfilled") {
    candidateSelection.value = candidatesResult.value;
  } else {
    candidateSelection.value = null;
    loadWarnings.value.push(
      `Kandidaten: ${toUserMessage(candidatesResult.reason, "Die Kandidaten konnten nicht geladen werden.")}`,
    );
  }

  if (matchesResult.status === "fulfilled") {
    matching.value = matchesResult.value;
  } else {
    matching.value = null;
    loadWarnings.value.push(
      `Matches: ${toUserMessage(matchesResult.reason, "Die Matches konnten nicht geladen werden.")}`,
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
</script>

<template>
  <section class="dashboard page-stack">
    <header class="dashboard__hero page-card">
      <div class="dashboard__hero-copy">
        <p class="eyebrow">
          Dashboard
        </p>
        <h1 class="page-title">
          {{ heroTitle }}
        </h1>
        <p class="page-copy">
          {{ heroCopy }}
        </p>
      </div>

      <div class="dashboard__hero-actions">
        <RouterLink
          :to="{ name: 'media-create' }"
          class="button button--primary"
        >
          Medium anlegen
        </RouterLink>
        <RouterLink
          :to="{ name: 'matches' }"
          class="button button--secondary"
        >
          Matches ansehen
        </RouterLink>
      </div>
    </header>

    <AppMessage
      v-if="loading"
      title="Dashboard wird geladen"
      description="Medien, Profil, Kandidaten und Matches werden parallel aus den bestehenden API-Aufrufen vorbereitet."
      tone="info"
    />

    <template v-else>
      <AppMessage
        v-if="loadWarnings.length > 0"
        title="Nicht alle Bereiche konnten geladen werden"
        :description="loadWarnings.join(' ')"
        tone="warning"
      >
        <div class="dashboard__message-actions">
          <button
            class="button button--secondary"
            type="button"
            @click="loadDashboard"
          >
            Erneut versuchen
          </button>
        </div>
      </AppMessage>

      <section class="dashboard__grid">
        <DashboardSummaryCard
          eyebrow="Medien"
          :value="String(media?.length ?? '–')"
          title="Lokale Mediensammlung"
          :description="
            media
              ? `${consumedCount} konsumiert, ${media.length - consumedCount} weitere Eintraege mit lokal bestaetigten Daten.`
              : 'Medien konnten in dieser Uebersicht nicht geladen werden.'
          "
          :link-to="{ name: 'media-list' }"
          link-label="Zur Sammlung"
          tone="info"
        />

        <DashboardSummaryCard
          eyebrow="Profil"
          :value="
            profile
              ? `${profile.profileRelevantMediaCount}/${profile.requiredProfileRelevantMediaCount}`
              : '–'
          "
          title="Matching-Basis"
          :description="
            profile
              ? `${profile.isReadyForMatching ? 'Bereit fuer Vergleiche.' : 'Noch nicht bereit.'} ${profileTagCount} gewichtete Tags sind aktuell sichtbar.`
              : 'Das Interessenprofil konnte in dieser Uebersicht nicht geladen werden.'
          "
          :link-to="{ name: 'profile' }"
          link-label="Profil ansehen"
          :tone="profile?.isReadyForMatching ? 'success' : 'warning'"
        />

        <DashboardSummaryCard
          eyebrow="Kandidaten"
          :value="String(candidateSelection?.candidates.length ?? '–')"
          title="Vorschlaege fuer spaeter"
          :description="
            candidateSelection
              ? `${candidateReadyCount} matching-bereit, ${candidateSelection.candidates.length - candidateReadyCount} noch ohne vollstaendige Tag-Basis.`
              : 'Die Kandidaten konnten in dieser Uebersicht nicht geladen werden.'
          "
          :link-to="{ name: 'candidates' }"
          link-label="Kandidaten pruefen"
          tone="default"
        />

        <DashboardSummaryCard
          eyebrow="Matches"
          :value="String(matching?.matches.length ?? '–')"
          title="Erklaerbare Vergleiche"
          :description="
            matching
              ? `${meaningfulMatchCount} mit Prozentangabe. ${matching.scoresSuppressed ? 'Scores sind derzeit noch unterdrueckt.' : 'Keine neue Backend-Logik noetig.'}`
              : 'Die Match-Ergebnisse konnten in dieser Uebersicht nicht geladen werden.'
          "
          :link-to="{ name: 'matches' }"
          link-label="Matches lesen"
          :tone="matching?.scoresSuppressed ? 'warning' : 'success'"
        />
      </section>

      <section class="dashboard__detail-grid">
        <article class="page-card dashboard__detail-card">
          <p class="eyebrow">
            Naechster sinnvoller Schritt
          </p>
          <h2 class="section-title">
            {{
              profile?.isReadyForMatching
                ? "Match-Karten vergleichen"
                : "Profil weiter fuellen"
            }}
          </h2>
          <p class="body-muted">
            {{
              profile?.isReadyForMatching
                ? "Die Match-Ansicht zeigt dir jetzt Ueberschneidungen, unvollstaendige Kandidaten und bewusste No-Score-Faelle nebeneinander."
                : "Fuer aussagekraeftige Prozentwerte brauchst du mindestens drei konsumierte Medien mit Bewertung ab 4 und bestaetigten Tags."
            }}
          </p>
        </article>

        <article class="page-card dashboard__detail-card">
          <p class="eyebrow">
            Aktueller Fokus
          </p>
          <h2 class="section-title">
            {{
              bestMatch?.candidate.media.title ??
                "Noch kein fuehrender Match-Kandidat"
            }}
          </h2>
          <p class="body-muted">
            {{
              bestMatch
                ? bestMatch.explanationMessage
                : "Sobald Kandidaten vorhanden sind, erscheint hier die aktuell bestplatzierte Match-Zusammenfassung aus dem bestehenden Match-API-Aufruf."
            }}
          </p>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.dashboard__hero {
  display: grid;
  gap: 1.5rem;
  padding: clamp(1.5rem, 3vw, 2rem);
  background: radial-gradient(
      circle at top right,
      color-mix(in srgb, var(--color-accent-soft) 85%, transparent),
      transparent 35%
    ),
    linear-gradient(
      180deg,
      color-mix(
        in srgb,
        var(--color-surface-secondary) 78%,
        var(--color-surface)
      ),
      var(--color-surface)
    );
}

.dashboard__hero-copy {
  display: grid;
  gap: 0.5rem;
}

.dashboard__hero-copy .page-copy {
  margin: 0;
}

.dashboard__hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.dashboard__grid,
.dashboard__detail-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.dashboard__detail-card {
  display: grid;
  gap: 0.5rem;
  padding: 1.35rem;
}

.dashboard__detail-card p {
  margin: 0;
}

.dashboard__message-actions {
  margin-top: 1rem;
}
</style>
