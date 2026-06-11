<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { ApiRequestError } from '@/api/client';
import { getProfile } from '@/api/profile';
import AppMessage from '@/components/common/AppMessage.vue';
import ProfileContributionCard from '@/components/profile/ProfileContributionCard.vue';
import InterestProfileWeights from '@/components/profile/InterestProfileWeights.vue';
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
          Dein Interessenprofil
        </h1>
        <p class="page-copy">
          Hier siehst du, welche konsumierten und positiv bewerteten Medien aktuell in das
          Matching einfliessen.
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
      <section class="profile-view__summary">
        <article class="page-card profile-view__summary-card">
          <p class="eyebrow">
            Bereit fuer Matching
          </p>
          <h2>{{ profile.isReadyForMatching ? 'Ja' : 'Noch nicht' }}</h2>
          <p class="body-muted">
            {{ profile.explanationMessage }}
          </p>
        </article>

        <article class="page-card profile-view__summary-card">
          <p class="eyebrow">
            Relevante Medien
          </p>
          <h2>{{ profile.profileRelevantMediaCount }} / {{ profile.requiredProfileRelevantMediaCount }}</h2>
          <p class="body-muted">
            Mindestens {{ profile.requiredProfileRelevantMediaCount }} benoetigt
          </p>
        </article>

        <article class="page-card profile-view__summary-card">
          <p class="eyebrow">
            Starke Tags
          </p>
          <h2>{{ strongestTags.length }}</h2>
          <p class="body-muted">
            Sichtbarer Ausschnitt der hoechsten Profilgewichte
          </p>
        </article>
      </section>

      <AppMessage
        v-if="!profile.isReadyForMatching"
        title="Noch zu wenig Profildaten"
        :description="profile.explanationMessage"
        tone="warning"
      />

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
.profile-view__summary {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.profile-view__summary-card {
  padding: 1.25rem;
}

.profile-view__summary-card h2,
.profile-view__summary-card p {
  margin: 0.35rem 0 0;
}

.profile-view__layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.35fr);
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
  .profile-view__summary,
  .profile-view__layout {
    grid-template-columns: 1fr;
  }
}
</style>
