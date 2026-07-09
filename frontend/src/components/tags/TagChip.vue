<script setup lang="ts">
import { computed } from "vue";

import type { TagResponse } from "@/types/api";

const props = defineProps<{
  tag: TagResponse;
  selected?: boolean;
}>();

const categoryTokenClass = computed(
  () => `tag-chip--${props.tag.category.toLowerCase()}`,
);
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
  --tag-chip-accent: color-mix(
    in srgb,
    var(--color-accent) 72%,
    var(--color-text-primary)
  );
  --tag-chip-border: color-mix(
    in srgb,
    var(--color-border-strong) 78%,
    transparent
  );
  --tag-chip-background: color-mix(
    in srgb,
    var(--color-surface-secondary) 76%,
    var(--tag-chip-accent)
  );
  --tag-chip-text: color-mix(in srgb, var(--color-text-primary) 86%, white);
  position: relative;
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  gap: 0.38rem;
  padding: 0.24rem 0.58rem;
  border: 1px solid var(--tag-chip-border);
  border-radius: var(--radius-full);
  color: var(--tag-chip-text);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), transparent 36%),
    var(--tag-chip-background);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 8px 18px rgba(10, 16, 28, 0.06);
  font-size: 0.79rem;
  font-weight: 700;
  line-height: 1.2;
  overflow-wrap: anywhere;
  text-shadow:
    0 1px 0 rgba(255, 255, 255, 0.16),
    0 0 0.62rem rgba(255, 255, 255, 0.28);
  white-space: normal;
}

.tag-chip::before {
  content: "";
  width: 0.36rem;
  height: 0.36rem;
  border-radius: 50%;
  background: var(--tag-chip-accent);
  opacity: 0.88;
  box-shadow: 0 0 0.8rem color-mix(in srgb, var(--tag-chip-accent) 46%, transparent);
}

.tag-chip--genre {
  --tag-chip-accent: #4e86d8;
  --tag-chip-border: color-mix(in srgb, #4e86d8 32%, var(--color-border));
  --tag-chip-background: color-mix(in srgb, #4e86d8 30%, var(--color-surface));
  --tag-chip-text: color-mix(in srgb, var(--color-text-primary) 82%, #f2f7ff);
}

.tag-chip--theme {
  --tag-chip-accent: #1e9b8d;
  --tag-chip-border: color-mix(in srgb, #1e9b8d 30%, var(--color-border));
  --tag-chip-background: color-mix(in srgb, #1e9b8d 28%, var(--color-surface));
  --tag-chip-text: color-mix(in srgb, var(--color-text-primary) 82%, #effffb);
}

.tag-chip--setting {
  --tag-chip-accent: #aa8b4d;
  --tag-chip-border: color-mix(in srgb, #aa8b4d 30%, var(--color-border));
  --tag-chip-background: color-mix(in srgb, #aa8b4d 20%, var(--color-surface));
  --tag-chip-text: color-mix(in srgb, var(--color-text-primary) 84%, #f5ead2);
}

.tag-chip--tone {
  --tag-chip-accent: #8759d2;
  --tag-chip-border: color-mix(in srgb, #8759d2 30%, var(--color-border));
  --tag-chip-background: color-mix(in srgb, #8759d2 28%, var(--color-surface));
  --tag-chip-text: color-mix(in srgb, var(--color-text-primary) 82%, #f7f1ff);
}

.tag-chip--experience {
  --tag-chip-accent: #6580b8;
  --tag-chip-border: color-mix(in srgb, #6580b8 28%, var(--color-border));
  --tag-chip-background: color-mix(in srgb, #6580b8 26%, var(--color-surface));
  --tag-chip-text: color-mix(in srgb, var(--color-text-primary) 84%, #f1f6ff);
}

.tag-chip--selected {
  --tag-chip-accent: color-mix(
    in srgb,
    var(--color-accent) 84%,
    var(--theme-badge-accent-text)
  );
  --tag-chip-border: color-mix(in srgb, var(--color-accent) 54%, var(--color-border));
  --tag-chip-background: color-mix(
    in srgb,
    var(--color-accent-soft) 74%,
    var(--color-surface)
  );
  --tag-chip-text: var(--theme-badge-accent-text);
}
</style>
