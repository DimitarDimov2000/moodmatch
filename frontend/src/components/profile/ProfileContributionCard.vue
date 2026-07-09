<script setup lang="ts">
import { i18n } from "@/i18n";
import { getDisplayText } from "@/components/media/media-presentation";
import TagChip from "@/components/tags/TagChip.vue";
import type { InterestProfileMediaContributionResponse } from "@/types/api";
import {
  getMatchingCommitmentLevelLabel,
  getMatchingMediaTypeLabel,
} from "@/components/matching/matching-format";
import { formatDecimal } from "@/components/matching/matching-format";

defineProps<{
  contribution: InterestProfileMediaContributionResponse;
}>();
const { t } = i18n.global;
</script>

<template>
  <article class="profile-contribution-card page-card">
    <div class="profile-contribution-card__header">
      <div class="profile-contribution-card__copy-column">
        <p class="eyebrow">
          {{ t("profileContribution.eyebrow") }}
        </p>
        <h3 class="profile-contribution-card__title">
          {{ getDisplayText(contribution.media.title) }}
        </h3>
        <p class="profile-contribution-card__meta">
          {{ getMatchingMediaTypeLabel(contribution.media.mediaType) }} ·
          {{ getMatchingCommitmentLevelLabel(contribution.media.commitmentLevel) }} ·
          {{ t("profileContribution.rating", { value: contribution.media.rating ?? t("profileContribution.noRating") }) }}
        </p>
        <p class="profile-contribution-card__copy">
          {{ t("profileContribution.copy") }}
        </p>
      </div>

      <div class="profile-contribution-card__stats">
        <div class="profile-contribution-card__stat-card profile-contribution-card__stat-card--rating">
          <span class="profile-contribution-card__stat-label">{{ t("profileContribution.ratingSignal") }}</span>
          <strong class="profile-contribution-card__stat-value">
            {{
              formatDecimal(contribution.ratingWeight) ??
                contribution.ratingWeight
            }}
          </strong>
        </div>
        <div class="profile-contribution-card__stat-card profile-contribution-card__stat-card--favourite">
          <span class="profile-contribution-card__stat-label">{{ t("profileContribution.favouriteBonus") }}</span>
          <strong class="profile-contribution-card__stat-value">
            {{
              formatDecimal(contribution.favouriteFactor) ??
                contribution.favouriteFactor
            }}
          </strong>
        </div>
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
        <div class="profile-contribution-card__tag-metric">
          <span class="profile-contribution-card__value">
            {{
              formatDecimal(item.contributionWeight) ?? item.contributionWeight
            }}
          </span>
          <span class="profile-contribution-card__metric-label">{{ t("profileContribution.tagWeightLabel") }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.profile-contribution-card {
  display: grid;
  gap: 0.86rem;
  padding: clamp(0.92rem, 2.3vw, 1.08rem);
}

.profile-contribution-card__header {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 16.25rem);
  align-items: start;
}

.profile-contribution-card__copy-column {
  min-width: 0;
}

.profile-contribution-card__title,
.profile-contribution-card__meta,
.profile-contribution-card__copy {
  margin: 0;
}

.profile-contribution-card__title {
  margin-top: 0.35rem;
  overflow-wrap: anywhere;
  font-size: 1.15rem;
}

.profile-contribution-card__meta {
  color: var(--color-text-secondary);
}

.profile-contribution-card__copy {
  margin-top: 0.35rem;
  color: var(--color-text-secondary);
  font-size: 0.92rem;
}

.profile-contribution-card__stats {
  display: grid;
  gap: 0.5rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: min(100%, 16.5rem);
  align-self: start;
}

.profile-contribution-card__stat-card {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 0.16rem;
  min-height: 5.35rem;
  padding: 0.74rem 0.82rem;
  border: 1px solid color-mix(in srgb, var(--color-accent) 20%, var(--color-border));
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 34%),
    color-mix(in srgb, var(--color-accent-soft) 36%, var(--color-surface-secondary));
}

.profile-contribution-card__stat-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.16), transparent 40%);
  pointer-events: none;
}

.profile-contribution-card__stat-card--rating {
  border-color: color-mix(in srgb, #6f76cf 34%, var(--color-border));
  background:
    radial-gradient(circle at 88% 18%, rgba(192, 203, 255, 0.24), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 34%),
    color-mix(in srgb, #d9dcfb 42%, var(--color-surface-secondary));
  box-shadow: 0 10px 22px rgba(92, 100, 184, 0.08);
}

.profile-contribution-card__stat-card--favourite {
  border-color: color-mix(in srgb, #c99a46 34%, var(--color-border));
  background:
    radial-gradient(circle at 88% 18%, rgba(255, 215, 142, 0.24), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 34%),
    color-mix(in srgb, #efd6a5 44%, var(--color-surface-secondary));
  box-shadow: 0 10px 24px rgba(165, 120, 38, 0.09);
}

.profile-contribution-card__stat-label,
.profile-contribution-card__metric-label {
  color: color-mix(in srgb, var(--color-text-secondary) 82%, var(--color-text-muted));
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-contribution-card__stat-value {
  position: relative;
  z-index: 1;
  margin-top: auto;
  justify-self: start;
  font-size: 1.66rem;
  letter-spacing: 0;
  line-height: 0.95;
}

.profile-contribution-card__stat-card--rating .profile-contribution-card__stat-value {
  color: color-mix(in srgb, #5b5fb2 84%, var(--color-text-primary));
}

.profile-contribution-card__stat-card--favourite .profile-contribution-card__stat-value {
  color: color-mix(in srgb, #815912 84%, var(--color-text-primary));
}

.profile-contribution-card__tags {
  display: grid;
  gap: 0.42rem;
}

.profile-contribution-card__tag-row {
  display: grid;
  align-items: start;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.68rem;
  min-width: 0;
  padding: 0.52rem 0.66rem;
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 70%, transparent);
  border-radius: var(--radius-md);
  background:
    linear-gradient(90deg, color-mix(in srgb, #70b2d1 14%, transparent), transparent 48%),
    color-mix(in srgb, var(--color-surface-secondary) 82%, transparent);
}

.profile-contribution-card__tag-metric {
  display: grid;
  gap: 0.02rem;
  justify-items: start;
  min-width: 3.35rem;
  padding: 0.24rem 0.44rem;
  border: 1px solid color-mix(in srgb, #667fc2 26%, var(--color-border));
  border-radius: calc(var(--radius-md) - 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent 34%),
    color-mix(in srgb, #d8e2f4 24%, var(--color-surface));
}

.profile-contribution-card__value {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 1rem;
  padding: 0;
  border: 0;
  background: transparent;
  color: color-mix(in srgb, #8ca4e0 58%, var(--color-text-primary));
  font-size: 0.98rem;
  font-weight: 800;
  line-height: 1;
}

.profile-contribution-card__tag-row :deep(.tag-chip) {
  max-width: 100%;
  justify-self: start;
  overflow-wrap: anywhere;
  white-space: normal;
}

@media (max-width: 720px) {
  .profile-contribution-card__header {
    grid-template-columns: 1fr;
  }

  .profile-contribution-card__stats {
    grid-template-columns: 1fr;
    min-width: 0;
    width: 100%;
  }

  .profile-contribution-card__tag-row {
    grid-template-columns: 1fr;
  }

  .profile-contribution-card__tag-metric {
    justify-items: start;
  }

  .profile-contribution-card__value {
    justify-content: flex-start;
  }
}
</style>
