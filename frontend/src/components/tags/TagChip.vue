<script setup lang="ts">
import { computed } from 'vue';

import type { TagResponse } from '@/types/api';

const props = defineProps<{
  tag: TagResponse;
  selected?: boolean;
}>();

const categoryTokenClass = computed(() => `tag-chip--${props.tag.category.toLowerCase()}`);
</script>

<template>
  <span
    class="tag-chip"
    :class="[categoryTokenClass, { 'tag-chip--selected': selected }]"
  >
    {{ tag.name }}
  </span>
</template>

<style scoped>
.tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.45rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  background: var(--color-surface-secondary);
  font-size: 0.92rem;
}

.tag-chip--selected {
  border-color: var(--color-accent);
  color: var(--color-accent-dark);
  background: var(--color-accent-soft);
}

.tag-chip--genre {
  background: color-mix(in srgb, var(--color-info-soft) 75%, var(--color-surface));
}

.tag-chip--theme {
  background: color-mix(in srgb, var(--color-success-soft) 75%, var(--color-surface));
}

.tag-chip--setting {
  background: color-mix(in srgb, var(--color-warning-soft) 65%, var(--color-surface));
}

.tag-chip--tone {
  background: color-mix(in srgb, var(--color-accent-soft) 80%, var(--color-surface));
}

.tag-chip--experience {
  background: color-mix(in srgb, var(--color-surface-muted) 80%, var(--color-surface));
}
</style>

