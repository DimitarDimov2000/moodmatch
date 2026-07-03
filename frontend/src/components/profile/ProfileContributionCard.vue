<script setup lang="ts">
import TagChip from '@/components/tags/TagChip.vue';
import type { InterestProfileMediaContributionResponse } from '@/types/api';
import {
  commitmentLevelLabels,
  mediaTypeLabels,
} from '@/components/matching/matching-format';
import { formatDecimal } from '@/components/matching/matching-format';

defineProps<{
  contribution: InterestProfileMediaContributionResponse;
}>();
</script>

<template>
  <article class="profile-contribution-card page-card">
    <div class="profile-contribution-card__header">
      <div>
        <p class="eyebrow">
          Profilquelle
        </p>
        <h3 class="profile-contribution-card__title">
          {{ contribution.media.title }}
        </h3>
        <p class="profile-contribution-card__meta">
          {{ mediaTypeLabels[contribution.media.mediaType] }} ·
          {{ commitmentLevelLabels[contribution.media.commitmentLevel] }} ·
          Bewertung {{ contribution.media.rating ?? 'keine' }}
        </p>
        <p class="profile-contribution-card__copy">
          Dieses Medium staerkt dein Profil ueber die unten sichtbaren Tags.
        </p>
      </div>

      <div class="profile-contribution-card__stats">
        <span>Bewertungssignal {{ formatDecimal(contribution.ratingWeight) ?? contribution.ratingWeight }}</span>
        <span>Favoriten-Bonus {{ formatDecimal(contribution.favouriteFactor) ?? contribution.favouriteFactor }}</span>
      </div>
    </div>

    <div
      v-if="contribution.tagContributions.length"
      class="profile-contribution-card__tags"
    >
      <div
        v-for="item in contribution.tagContributions"
        :key="item.tag.id"
        class="profile-contribution-card__tag-row"
      >
        <TagChip :tag="item.tag" />
        <span class="profile-contribution-card__value">
          {{ formatDecimal(item.contributionWeight) ?? item.contributionWeight }}
        </span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.profile-contribution-card {
  display: grid;
  gap: 1rem;
  padding: clamp(1.15rem, 2.8vw, 1.5rem);
}

.profile-contribution-card__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
}

.profile-contribution-card__title,
.profile-contribution-card__meta,
.profile-contribution-card__copy {
  margin: 0;
}

.profile-contribution-card__title {
  margin-top: 0.35rem;
  font-size: 1.15rem;
}

.profile-contribution-card__meta {
  color: var(--color-text-secondary);
}

.profile-contribution-card__copy {
  margin-top: 0.6rem;
  color: var(--color-text-secondary);
}

.profile-contribution-card__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.profile-contribution-card__stats span {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--radius-full);
  background: var(--color-surface-secondary);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  font-weight: 600;
}

.profile-contribution-card__tags {
  display: grid;
  gap: 0.7rem;
}

.profile-contribution-card__tag-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-secondary);
}

.profile-contribution-card__value {
  font-weight: 700;
}
</style>
