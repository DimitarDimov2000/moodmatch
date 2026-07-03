<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { ApiRequestError } from "@/api/client";
import { getProfile } from "@/api/profile";
import AppMessage from "@/components/common/AppMessage.vue";
import ProfileContributionCard from "@/components/profile/ProfileContributionCard.vue";
import InterestProfileWeights from "@/components/profile/InterestProfileWeights.vue";
import TagChip from "@/components/tags/TagChip.vue";
import type { InterestProfileResponse } from "@/types/api";

const profile = ref<InterestProfileResponse | null>(null);
const loading = ref(true);
const errorMessage = ref("");

const strongestTags = computed(
  () => profile.value?.weightedTags.slice(0, 6) ?? [],
);
const contributionCount = computed(
  () => profile.value?.contributingMedia.length ?? 0,
);

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

  return "Das Interessenprofil konnte nicht geladen werden.";
}
</script>

<template>
  <section class="page-stack">
    <header class="page-header">
      <div>
        <p class="eyebrow">
          Profil
        </p>
        <h1 class="page-title">
          So versteht MoodMatch deinen Geschmack
        </h1>
        <p class="page-copy">
          Hier siehst du kompakt, welche Signale dein Profil tragen und warum
          einzelne Empfehlungen dadurch besser zu dir passen.
        </p>
      </div>
    </header>

    <AppMessage
      v-if="loading"
      title="Profil wird geladen"
      description="Gewichtete Tags und Profilbeitraege werden vorbereitet."
      tone="info"
    />

    <AppMessage
      v-else-if="errorMessage"
      title="Profil konnte nicht geladen werden"
      :description="errorMessage"
      tone="error"
    >
      <div class="profile-view__message-actions">
        <button
          class="button button--secondary"
          type="button"
          @click="loadProfile"
        >
          Erneut versuchen
        </button>
      </div>
    </AppMessage>

    <template v-else-if="profile">
      <section class="overview-stats">
        <article class="page-card overview-stat-card">
          <p class="eyebrow">
            Bereit fuer Matching
          </p>
          <p class="overview-stat-card__value">
            {{ profile.isReadyForMatching ? "Ja" : "Noch nicht" }}
          </p>
          <p class="overview-stat-card__copy">
            {{ profile.explanationMessage }}
          </p>
        </article>

        <article class="page-card overview-stat-card">
          <p class="eyebrow">
            Relevante Medien
          </p>
          <p class="overview-stat-card__value">
            {{ profile.profileRelevantMediaCount }} /
            {{ profile.requiredProfileRelevantMediaCount }}
          </p>
          <p class="overview-stat-card__copy">
            Mindestens {{ profile.requiredProfileRelevantMediaCount }} benoetigt
          </p>
        </article>

        <article class="page-card overview-stat-card">
          <p class="eyebrow">
            Starke Signale
          </p>
          <p class="overview-stat-card__value">
            {{ strongestTags.length }}
          </p>
          <p class="overview-stat-card__copy">
            Sichtbare Schwerpunkt-Tags
          </p>
        </article>
      </section>

      <AppMessage
        v-if="!profile.isReadyForMatching"
        title="Noch zu wenig Profildaten"
        :description="profile.explanationMessage"
        tone="warning"
      >
        <div class="state-actions">
          <RouterLink
            :to="{ name: 'media-list' }"
            class="button button--secondary"
          >
            Mediathek oeffnen
          </RouterLink>
        </div>
      </AppMessage>

      <section class="page-card profile-view__summary">
        <div class="section-header">
          <div class="section-header__copy">
            <p class="eyebrow">
              Zusammenfassung
            </p>
            <h2 class="section-title">
              Deine staerksten Geschmackssignale
            </h2>
            <p class="body-muted">
              {{ profile.explanationMessage }}
            </p>
          </div>
        </div>

        <div class="profile-view__summary-grid">
          <div class="profile-view__spotlight">
            <p class="profile-view__spotlight-label">
              Deine staerksten Geschmackssignale
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
              Sobald mehr konsumierte Medien mit bestaetigten Tags vorliegen,
              werden hier deine staerksten Signale sichtbar.
            </p>
          </div>

          <div class="profile-view__summary-notes">
            <div class="profile-view__summary-note">
              <p class="profile-view__spotlight-label">
                Was deine Empfehlungen praegt
              </p>
              <p class="body-muted">
                Positive Bewertungen, Favoriten und bestaetigte Tags staerken
                die Signale, die spaeter in Matches sichtbar werden.
              </p>
            </div>

            <div class="profile-view__summary-note">
              <p class="profile-view__spotlight-label">
                Woher dieses Signal kommt
              </p>
              <p class="body-muted">
                {{ contributionCount }} Medien tragen aktuell zu deinem Profil
                bei.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section class="profile-view__layout">
        <InterestProfileWeights
          class="profile-view__weights"
          :tags="profile.weightedTags"
          title="Deine staerksten Geschmackssignale"
        />

        <section class="profile-view__media">
          <div class="profile-view__section-header">
            <h2 class="section-title">
              Was deine Empfehlungen praegt
            </h2>
            <p class="body-muted">
              Woher dieses Signal kommt: konsumierte Medien mit Bewertung,
              Favoritenstatus und bestaetigten Tags.
            </p>
          </div>

          <AppMessage
            v-if="profile.contributingMedia.length === 0"
            title="Noch keine Profilquellen"
            description="Sobald konsumierte Medien mit positiver Bewertung und bestaetigten Tags vorhanden sind, erscheinen sie hier kompakt erklaert."
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
