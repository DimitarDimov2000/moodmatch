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
import {
  consumptionStatusLabels,
  mediaTypeLabels,
} from "@/components/matching/matching-format";
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
const mediaCount = computed(() => media.value?.length ?? 0);
const candidateCount = computed(
  () => candidateSelection.value?.candidates.length ?? 0,
);
const bestMatch = computed<MatchResultResponse | null>(
  () => matching.value?.matches[0] ?? null,
);
const recentMediaItems = computed(() =>
  [...(media.value ?? [])]
    .sort(
      (left, right) =>
        new Date(right.updatedAt).getTime() -
        new Date(left.updatedAt).getTime(),
    )
    .slice(0, 3),
);

const heroTitle = computed(() => {
  if (mediaCount.value === 0 && candidateCount.value === 0) {
    return "Willkommen in deinem MoodMatch-Ueberblick.";
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return "Dein Profil ist fast bereit fuer aussagekraeftige Matches.";
  }

  if (matching.value?.scoresSuppressed) {
    return "Deine Kandidatenliste steht, die Vergleichsbasis waechst noch.";
  }

  if (bestMatch.value?.relativeScore !== null) {
    return `${bestMatch.value?.candidate.media.title} fuehrt aktuell deine Matches an.`;
  }

  return "Dein Home-Bereich zeigt Sammlung, Profil und Empfehlungen auf einen Blick.";
});

const heroCopy = computed(() => {
  if (mediaCount.value === 0 && candidateCount.value === 0) {
    return "Sobald du erste Medien oder Wunschkandidaten anlegst, fuehrt dich dieser Bereich direkt zu Profilaufbau, Sammlung und Match-Vergleich.";
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return `${profile.value.explanationMessage} Konzentriere dich jetzt auf konsumierte Medien mit Bewertung und bestaetigten Tags.`;
  }

  if (matching.value?.scoresSuppressed) {
    return `${matching.value.explanationMessage} Ohne genug Vergleichsdaten bleibt MoodMatch bewusst vorsichtig.`;
  }

  if (bestMatch.value?.relativeScore !== null) {
    return "Die Prozentzahl ist bewusst relativ und erscheint nur dann, wenn mehrere Kandidaten sinnvoll miteinander verglichen werden koennen.";
  }

  return "Nutze diesen Bereich, um schnell zu sehen, wo dein Profil steht und welcher naechste Schritt gerade am meisten bringt.";
});

const nextAction = computed(() => {
  if (mediaCount.value === 0) {
    return {
      title: "Erstes Medium anlegen",
      copy: "Lege ein konsumiertes Medium oder einen Wunschkandidaten an, damit Sammlung, Profil und spaetere Matches sichtbar werden.",
      to: { name: "media-create" as const },
      label: "Medium anlegen",
    };
  }

  if (profile.value && !profile.value.isReadyForMatching) {
    return {
      title: "Profil weiter fuellen",
      copy: "Bewerte weitere konsumierte Medien und bestaetige Tags, damit aus einzelnen Eindruecken ein belastbares Geschmacksprofil wird.",
      to: { name: "profile" as const },
      label: "Profil ansehen",
    };
  }

  if (candidateCount.value === 0) {
    return {
      title: "Kandidaten sammeln",
      copy: "Sobald du Wunschkandidaten hinterlegst, kann MoodMatch sie mit deinem Profil vergleichen und spaeter priorisieren.",
      to: { name: "media-create" as const },
      label: "Kandidat anlegen",
    };
  }

  return {
    title: "Matches vergleichen",
    copy: "Dein Profil ist bereit. Vergleiche jetzt die staerksten Vorschlaege und sieh dir an, warum einzelne Titel gut zu dir passen.",
    to: { name: "matches" as const },
    label: "Zu den Matches",
  };
});

const focusCard = computed(() => {
  if (bestMatch.value) {
    return {
      title: bestMatch.value.candidate.media.title,
      copy: bestMatch.value.explanationMessage,
      to: { name: "matches" as const },
      label: "Match lesen",
    };
  }

  if (candidateCount.value > 0) {
    return {
      title: "Kandidaten warten auf Vergleich",
      copy: "Deine Liste ist vorhanden. Sobald Profil und Tag-Basis stark genug sind, werden hier klare Favoriten sichtbar.",
      to: { name: "candidates" as const },
      label: "Kandidaten ansehen",
    };
  }

  return {
    title: "Noch kein Match-Favorit",
    copy: "Mit mehr Profilsignalen und Kandidaten zeigt dir dieser Bereich spaeter den aktuell vielversprechendsten Vorschlag.",
    to: { name: "candidates" as const },
    label: "Zur Kandidatenliste",
  };
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

        <div class="dashboard__hero-badges">
          <span class="badge"> {{ mediaCount }} Medien </span>
          <span
            class="badge"
            :class="
              profile?.isReadyForMatching ? 'badge--success' : 'badge--warning'
            "
          >
            {{
              profile?.isReadyForMatching ? "Profil bereit" : "Profil im Aufbau"
            }}
          </span>
          <span
            class="badge"
            :class="meaningfulMatchCount > 0 ? 'badge--accent' : ''"
          >
            {{ meaningfulMatchCount }} aussagekraeftige Matches
          </span>
        </div>
      </div>

      <aside class="dashboard__hero-side">
        <article class="dashboard__hero-panel">
          <p class="eyebrow">
            Naechster Schritt
          </p>
          <h2 class="section-title">
            {{ nextAction.title }}
          </h2>
          <p class="body-muted">
            {{ nextAction.copy }}
          </p>

          <div class="dashboard__hero-panel-actions">
            <RouterLink
              :to="nextAction.to"
              class="button button--primary"
            >
              {{ nextAction.label }}
            </RouterLink>
            <RouterLink
              :to="focusCard.to"
              class="button button--secondary"
            >
              {{ focusCard.label }}
            </RouterLink>
          </div>
        </article>

        <div class="dashboard__quick-actions">
          <RouterLink
            :to="{ name: 'media-list' }"
            class="button button--secondary"
          >
            Mediathek
          </RouterLink>
          <RouterLink
            :to="{ name: 'profile' }"
            class="button button--secondary"
          >
            Profil
          </RouterLink>
          <RouterLink
            :to="{ name: 'swipe' }"
            class="button button--secondary"
          >
            Swipe
          </RouterLink>
        </div>
      </aside>
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
        :description="loadWarnings.join('\n')"
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

      <section class="page-section">
        <div class="section-header">
          <div class="section-header__copy">
            <p class="eyebrow">
              Uebersicht
            </p>
            <h2 class="section-title">
              Wo dein MoodMatch gerade steht
            </h2>
            <p class="body-muted">
              Vier kompakte Karten zeigen Status, Profilreife und
              Empfehlungslage.
            </p>
          </div>
        </div>

        <div class="dashboard__grid">
          <DashboardSummaryCard
            eyebrow="Medien"
            :value="String(media?.length ?? '–')"
            title="Lokale Mediensammlung"
            :description="
              media
                ? `${consumedCount} konsumiert, ${media.length - consumedCount} weitere Eintraege in deiner Sammlung.`
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
                ? `${profile.isReadyForMatching ? 'Bereit fuer Vergleiche.' : 'Noch nicht bereit.'} ${profileTagCount} sichtbare Schwerpunkt-Tags.`
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
                ? `${candidateReadyCount} vergleichsbereit, ${candidateSelection.candidates.length - candidateReadyCount} brauchen noch mehr erwartete Tags.`
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
                ? `${meaningfulMatchCount} mit Prozentangabe. ${matching.scoresSuppressed ? 'Scores warten noch auf mehr Vergleichsdaten.' : 'Begruendungen und Prozentwerte sind bereit.'}`
                : 'Die Match-Ergebnisse konnten in dieser Uebersicht nicht geladen werden.'
            "
            :link-to="{ name: 'matches' }"
            link-label="Matches lesen"
            :tone="matching?.scoresSuppressed ? 'warning' : 'success'"
          />
        </div>
      </section>

      <section class="dashboard__detail-grid">
        <article class="page-card dashboard__detail-card">
          <p class="eyebrow">
            Aktueller Fokus
          </p>
          <h2 class="section-title">
            {{ focusCard.title }}
          </h2>
          <p class="body-muted">
            {{ focusCard.copy }}
          </p>
          <RouterLink
            :to="focusCard.to"
            class="button button--secondary"
          >
            {{ focusCard.label }}
          </RouterLink>
        </article>

        <article class="page-card dashboard__detail-card">
          <div class="dashboard__detail-heading">
            <p class="eyebrow">
              Zuletzt wichtig
            </p>
            <h2 class="section-title">
              {{
                recentMediaItems.length > 0
                  ? "Neu in deiner Sammlung"
                  : "Sammlung aufbauen"
              }}
            </h2>
            <p class="body-muted">
              {{
                recentMediaItems.length > 0
                  ? "Die juengsten Eintraege bleiben schnell erreichbar, ohne die Mediathek zu ueberladen."
                  : "Sobald du erste Medien anlegst, erscheint hier ein kompakter Rueckblick auf deine Sammlung."
              }}
            </p>
          </div>

          <ul
            v-if="recentMediaItems.length > 0"
            class="dashboard__recent-list"
          >
            <li
              v-for="item in recentMediaItems"
              :key="item.id"
              class="dashboard__recent-item"
            >
              <div>
                <p class="dashboard__recent-title">
                  {{ item.title }}
                </p>
                <p class="dashboard__recent-meta">
                  {{ mediaTypeLabels[item.mediaType] }} ·
                  {{ consumptionStatusLabels[item.consumptionStatus]
                  }}<span v-if="item.releaseYear">
                    · {{ item.releaseYear }}</span>
                </p>
              </div>
              <span class="badge"> {{ item.tags.length }} Tags </span>
            </li>
          </ul>

          <RouterLink
            :to="{ name: 'media-list' }"
            class="button button--secondary"
          >
            Zur Mediathek
          </RouterLink>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.dashboard__hero {
  display: grid;
  gap: 0.95rem;
  grid-template-columns: minmax(0, 1.45fr) minmax(260px, 0.9fr);
  padding: clamp(1.08rem, 2.3vw, 1.4rem);
  background: radial-gradient(
      circle at top right,
      color-mix(in srgb, var(--color-accent-soft) 58%, transparent),
      transparent 31%
    ),
    linear-gradient(
      180deg,
      color-mix(
        in srgb,
        var(--color-surface-secondary) 72%,
        var(--color-surface)
      ),
      var(--color-surface)
    );
}

.dashboard__hero-copy {
  display: grid;
  gap: 0.55rem;
}

.dashboard__hero-copy .page-copy {
  margin: 0;
}

.dashboard__hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.dashboard__hero-side {
  display: grid;
  gap: 0.75rem;
  align-content: start;
}

.dashboard__hero-panel,
.dashboard__detail-card {
  display: grid;
  gap: 0.72rem;
}

.dashboard__hero-panel {
  padding: 0.92rem;
  border: 1px solid var(--color-border);
  border-radius: calc(var(--radius-lg) - 6px);
  background: color-mix(
    in srgb,
    var(--color-surface-secondary) 76%,
    var(--color-surface)
  );
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.dashboard__grid,
.dashboard__detail-grid {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.dashboard__detail-grid {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.dashboard__detail-card {
  padding: 1rem;
}

.dashboard__detail-heading {
  display: grid;
  gap: 0.3rem;
}

.dashboard__recent-list {
  display: grid;
  gap: 0.6rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.dashboard__recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.72rem 0.82rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: color-mix(
    in srgb,
    var(--color-surface-secondary) 74%,
    transparent
  );
}

.dashboard__hero-panel p,
.dashboard__detail-card p {
  margin: 0;
}

.dashboard__hero-panel-actions,
.dashboard__quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.dashboard__quick-actions > * {
  flex: 1 1 8rem;
}

.dashboard__recent-title,
.dashboard__recent-meta {
  margin: 0;
}

.dashboard__recent-title {
  font-weight: 700;
}

.dashboard__recent-meta {
  margin-top: 0.2rem;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.dashboard__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .dashboard__hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard__quick-actions > * {
    flex: 1 1 10rem;
  }
}

@media (max-width: 560px) {
  .dashboard__hero-panel-actions > *,
  .dashboard__quick-actions > * {
    width: 100%;
  }

  .dashboard__recent-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
