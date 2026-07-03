<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';

import { ApiRequestError } from '@/api/client';
import { getProfile } from '@/api/profile';
import AppMessage from '@/components/common/AppMessage.vue';
import ProfileContributionCard from '@/components/profile/ProfileContributionCard.vue';
import InterestProfileWeights from '@/components/profile/InterestProfileWeights.vue';
import TagChip from '@/components/tags/TagChip.vue';
import type { InterestProfileResponse } from '@/types/api';

const profile = ref<InterestProfileResponse | null>(null);
const loading = ref(true);
const errorMessage = ref('');

const strongestTags = computed(() => profile.value?.weightedTags.slice(0, 6) ?? []);

onMounted(async () => {
  await loadProfile();
});

async function loadProfile() {
  loading.value = true;
  errorMessage.value = '';

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

  return 'Das Interessenprofil konnte nicht geladen werden.';
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
          Hier siehst du, welche konsumierten und positiv bewerteten Medien aktuell in dein
          Profil einfliessen und welche Tags davon am staerksten gepraegt werden.
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
            {{ profile.isReadyForMatching ? 'Ja' : 'Noch nicht' }}
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
            {{ profile.profileRelevantMediaCount }} / {{ profile.requiredProfileRelevantMediaCount }}
          </p>
          <p class="overview-stat-card__copy">
            Mindestens {{ profile.requiredProfileRelevantMediaCount }} benoetigt
          </p>
        </article>

        <article class="page-card overview-stat-card">
          <p class="eyebrow">
            Starke Tags
          </p>
          <p class="overview-stat-card__value">
            {{ strongestTags.length }}
          </p>
          <p class="overview-stat-card__copy">
            Aktuelle Schwerpunkte in deinem Profil
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

      <section class="page-card profile-view__guide">
        <div class="section-header">
          <div class="section-header__copy">
            <p class="eyebrow">
              Profil-Lesart
            </p>
            <h2 class="section-title">
              Was diese Seite zeigt
            </h2>
            <p class="body-muted">
              MoodMatch nutzt keine freie Interpretation: Sichtbar wird nur, was aus deinen
              vorhandenen Bewertungen, Favoriten und bestaetigten Tags hervorgeht.
            </p>
          </div>
        </div>

        <div
          v-if="strongestTags.length > 0"
          class="profile-view__spotlight"
        >
          <p class="profile-view__spotlight-label">
            Aktuelle Schwerpunkt-Tags
          </p>
          <div class="profile-view__spotlight-tags">
            <TagChip
              v-for="item in strongestTags"
              :key="item.tag.id"
              :tag="item.tag"
            />
          </div>
        </div>
      </section>

      <section class="profile-view__layout">
        <InterestProfileWeights
          class="profile-view__weights"
          :tags="profile.weightedTags"
        />

        <section class="profile-view__media">
          <div class="profile-view__section-header">
            <h2 class="section-title">
              Medien, die das Profil praegen
            </h2>
            <p class="body-muted">
              Bewertungs- und Favoritenfaktoren kommen unveraendert aus dem Backend.
            </p>
          </div>

          <AppMessage
            v-if="profile.contributingMedia.length === 0"
            title="Noch keine Profilquellen"
            description="Sobald konsumierte Medien mit positiver Bewertung und bestaetigten Tags vorhanden sind, erscheinen sie hier."
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
  gap: 1.5rem;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.35fr);
}

.profile-view__guide {
  display: grid;
  gap: 1rem;
  padding: clamp(1.15rem, 2.8vw, 1.45rem);
}

.profile-view__spotlight {
  display: grid;
  gap: 0.65rem;
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
  gap: 0.65rem;
}

.profile-view__media,
.profile-view__contributions {
  display: grid;
  gap: 1rem;
}

.profile-view__section-header p {
  margin: 0.4rem 0 0;
}

.profile-view__message-actions {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .profile-view__layout {
    grid-template-columns: 1fr;
  }
}
</style>
