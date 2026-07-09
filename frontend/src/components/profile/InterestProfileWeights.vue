<script setup lang="ts">
import { computed } from "vue";

import TagChip from "@/components/tags/TagChip.vue";
import { i18n } from "@/i18n";
import type { InterestProfileTagWeightResponse } from "@/types/api";
import { formatDecimal } from "@/components/matching/matching-format";

const props = defineProps<{
  title?: string;
  tags: InterestProfileTagWeightResponse[];
}>();
const { t } = i18n.global;

const strongestWeight = computed(() =>
  props.tags.reduce((currentMax, item) => {
    const parsed = Number(item.weight);

    if (Number.isNaN(parsed)) {
      return currentMax;
    }

    return Math.max(currentMax, parsed);
  }, 0),
);

function getWeightBarWidth(weight: string): string {
  const parsed = Number(weight);

  if (Number.isNaN(parsed) || strongestWeight.value <= 0) {
    return "14%";
  }

  return `${Math.max(Math.round((parsed / strongestWeight.value) * 100), 14)}%`;
}
</script>

<template>
  <section class="interest-profile-weights page-card">
    <div class="interest-profile-weights__header">
      <div class="interest-profile-weights__copy">
        <p class="eyebrow">
          {{ t("profileWeights.eyebrow") }}
        </p>
        <h2 class="section-title">
          {{ title ?? t("profileWeights.defaultTitle") }}
        </h2>
        <p class="body-muted">
          {{ t("profileWeights.intro") }}
        </p>
      </div>
    </div>

    <p
      v-if="tags.length === 0"
      class="body-muted"
    >
      {{ t("profileWeights.empty") }}
    </p>

    <ul
      v-else
      class="interest-profile-weights__list"
    >
      <li
        v-for="(item, index) in tags"
        :key="item.tag.id"
        class="interest-profile-weights__item"
      >
        <div class="interest-profile-weights__item-head">
          <span class="interest-profile-weights__rank">
            #{{ index + 1 }}
          </span>
          <TagChip :tag="item.tag" />
          <div class="interest-profile-weights__metric">
            <span class="interest-profile-weights__value">
              {{ formatDecimal(item.weight) ?? item.weight }}
            </span>
            <span class="interest-profile-weights__metric-label">
              {{ t("profileWeights.weightLabel") }}
            </span>
          </div>
        </div>

        <div
          class="interest-profile-weights__bar"
          aria-hidden="true"
        >
          <span :style="{ width: getWeightBarWidth(item.weight) }" />
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.interest-profile-weights {
  display: grid;
  gap: 0.95rem;
  padding: 1.05rem;
}

.interest-profile-weights__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.interest-profile-weights__copy {
  display: grid;
  gap: 0.35rem;
}

.interest-profile-weights__copy p {
  margin: 0;
}

.interest-profile-weights__list {
  display: grid;
  gap: 0.5rem;
  grid-template-columns: 1fr;
  margin: 0;
  padding: 0;
  list-style: none;
}

.interest-profile-weights__item {
  display: grid;
  gap: 0.5rem;
  padding: 0.68rem 0.76rem;
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 76%, transparent);
  border-radius: calc(var(--radius-md) + 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 34%),
    color-mix(in srgb, var(--color-surface-secondary) 84%, var(--color-surface));
}

.interest-profile-weights__item-head {
  display: grid;
  gap: 0.54rem;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  min-width: 0;
}

.interest-profile-weights__rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.65rem;
  min-height: 1.65rem;
  padding: 0.12rem 0.36rem;
  border: 1px solid color-mix(in srgb, #7a8ea6 32%, var(--color-border));
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--color-surface-secondary) 82%, var(--color-surface));
  color: color-mix(in srgb, #4e6076 82%, var(--color-text-primary));
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.interest-profile-weights__metric {
  display: grid;
  gap: 0.02rem;
  justify-items: start;
  min-width: 3.55rem;
  padding: 0.26rem 0.48rem;
  border: 1px solid color-mix(in srgb, #7892cc 28%, var(--color-border));
  border-radius: calc(var(--radius-md) - 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), transparent 34%),
    color-mix(in srgb, #dce6f7 28%, var(--color-surface));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.interest-profile-weights__metric-label {
  color: color-mix(in srgb, #7f90aa 62%, var(--color-text-secondary));
  font-size: 0.64rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1.08;
  text-transform: uppercase;
}

.interest-profile-weights__value {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 1.1rem;
  padding: 0;
  border: 0;
  background: transparent;
  color: color-mix(in srgb, #8ba6e4 58%, var(--color-text-primary));
  font-size: 1.06rem;
  font-weight: 800;
  line-height: 1;
  text-align: left;
}

.interest-profile-weights__item :deep(.tag-chip) {
  max-width: 100%;
  justify-self: start;
  overflow-wrap: anywhere;
  white-space: normal;
}

.interest-profile-weights__bar {
  width: 100%;
  height: 0.3rem;
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--color-surface-muted) 88%, transparent);
  overflow: hidden;
}

.interest-profile-weights__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(
    90deg,
    color-mix(in srgb, #557ec8 86%, white),
    color-mix(in srgb, #46a79b 72%, #d1f0ea),
    color-mix(in srgb, #855fd1 78%, #ddd0f9)
  );
}

@media (max-width: 720px) {
  .interest-profile-weights__item-head {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .interest-profile-weights__metric {
    grid-column: 1 / -1;
    justify-items: start;
  }

  .interest-profile-weights__value {
    justify-content: flex-start;
  }
}
</style>
