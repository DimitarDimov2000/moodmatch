<script setup lang="ts">
import TagChip from "@/components/tags/TagChip.vue";
import { i18n } from "@/i18n";
import type { InterestProfileTagWeightResponse } from "@/types/api";
import { formatDecimal } from "@/components/matching/matching-format";

defineProps<{
  title?: string;
  tags: InterestProfileTagWeightResponse[];
}>();
const { t } = i18n.global;
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
        v-for="item in tags"
        :key="item.tag.id"
        class="interest-profile-weights__item"
      >
        <TagChip :tag="item.tag" />
        <span class="interest-profile-weights__value">
          {{ formatDecimal(item.weight) ?? item.weight }}
        </span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.interest-profile-weights {
  display: grid;
  gap: 0.85rem;
  padding: 1.1rem;
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
  gap: 0.65rem;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  margin: 0;
  padding: 0;
  list-style: none;
}

.interest-profile-weights__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
  padding: 0.75rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-secondary);
}

.interest-profile-weights__value {
  min-width: 2.5rem;
  text-align: right;
  font-weight: 700;
  color: var(--color-text-primary);
}
</style>
