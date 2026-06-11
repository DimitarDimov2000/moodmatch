<script setup lang="ts">
import TagChip from '@/components/tags/TagChip.vue';
import type { InterestProfileTagWeightResponse } from '@/types/api';
import { formatDecimal } from '@/components/matching/matching-format';

defineProps<{
  title?: string;
  tags: InterestProfileTagWeightResponse[];
}>();
</script>

<template>
  <section class="interest-profile-weights page-card">
    <div class="interest-profile-weights__header">
      <div>
        <p class="eyebrow">
          Profil-Gewichte
        </p>
        <h2 class="section-title">
          {{ title ?? 'Interessenprofil nach Tags' }}
        </h2>
      </div>
      <p class="body-muted">
        Gewichte kommen direkt aus dem Backend und werden hier nur erklaert.
      </p>
    </div>

    <p
      v-if="tags.length === 0"
      class="body-muted"
    >
      Noch keine Tag-Gewichte verfuegbar.
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
  gap: 1rem;
  padding: 1.5rem;
}

.interest-profile-weights__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.interest-profile-weights__header p {
  margin: 0.35rem 0 0;
}

.interest-profile-weights__list {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  margin: 0;
  padding: 0;
  list-style: none;
}

.interest-profile-weights__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-secondary);
}

.interest-profile-weights__value {
  font-weight: 700;
  color: var(--color-text-primary);
}
</style>
