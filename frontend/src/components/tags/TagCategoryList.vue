<script setup lang="ts">
import { computed } from 'vue';

import TagChip from '@/components/tags/TagChip.vue';
import { groupTagsByCategory } from '@/components/tags/tag-utils';
import type { TagResponse } from '@/types/api';

const props = defineProps<{
  tags: TagResponse[];
  title?: string;
  compact?: boolean;
}>();

const groupedTags = computed(() => groupTagsByCategory(props.tags));
</script>

<template>
  <section class="tag-category-list page-card">
    <div class="tag-category-list__header">
      <h2 class="section-title">
        {{ title ?? 'Tags' }}
      </h2>
      <p class="body-muted">
        {{ tags.length }} Tags, gruppiert nach Kategorie.
      </p>
    </div>

    <div
      v-if="groupedTags.length"
      class="tag-category-list__groups"
      :class="{ 'tag-category-list__groups--compact': compact }"
    >
      <div
        v-for="group in groupedTags"
        :key="group.category"
        class="tag-category-list__group"
      >
        <h3 class="tag-category-list__group-title">
          {{ group.label }}
        </h3>
        <div class="tag-category-list__chips">
          <TagChip
            v-for="tag in group.tags"
            :key="tag.id"
            :tag="tag"
          />
        </div>
      </div>
    </div>

    <p
      v-else
      class="body-muted"
    >
      Noch keine Tags verfuegbar.
    </p>
  </section>
</template>

<style scoped>
.tag-category-list {
  display: grid;
  gap: 0.9rem;
  padding: 1.15rem;
}

.tag-category-list__header p {
  margin: 0.28rem 0 0;
}

.tag-category-list__groups {
  display: grid;
  gap: 0.85rem;
}

.tag-category-list__groups--compact {
  gap: 0.75rem;
}

.tag-category-list__group {
  display: grid;
  gap: 0.55rem;
}

.tag-category-list__group-title {
  margin: 0;
  font-size: 0.95rem;
}

.tag-category-list__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}
</style>
