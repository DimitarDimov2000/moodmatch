<script setup lang="ts">
import { storeToRefs } from 'pinia';

import { useAppStore } from '@/stores/app';

const appStore = useAppStore();
const { bootstrapSummary } = storeToRefs(appStore);

const readinessItems = [
  'Vue 3 + Vite + TypeScript bootstrapped',
  'Vue Router and Pinia registered in the app entry',
  'Vitest configured with jsdom for component tests',
];
</script>

<template>
  <section class="dashboard">
    <div class="dashboard__hero page-card">
      <p class="eyebrow">
        Frontend foundation
      </p>
      <h1 class="dashboard__title">
        Minimal, buildable MoodMatch shell
      </h1>
      <p class="dashboard__copy body-muted">
        {{ bootstrapSummary }}
      </p>
    </div>

    <div class="dashboard__grid">
      <article class="page-card dashboard__panel">
        <h2 class="section-title">
          Current scope
        </h2>
        <p class="body-muted">
          This first pass intentionally stops at app setup, shared layout, route wiring,
          and compile-safe placeholders for future API work.
        </p>
      </article>

      <article class="page-card dashboard__panel">
        <h2 class="section-title">
          Ready next
        </h2>
        <ul class="dashboard__list">
          <li
            v-for="item in readinessItems"
            :key="item"
          >
            {{ item }}
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 1.5rem;
}

.dashboard__hero,
.dashboard__panel {
  padding: 1.5rem;
}

.dashboard__title {
  margin: 0.5rem 0 0;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1.05;
}

.dashboard__copy {
  max-width: 42rem;
  margin: 1rem 0 0;
}

.dashboard__grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.dashboard__list {
  margin: 1rem 0 0;
  padding-left: 1.25rem;
  color: var(--color-text-secondary);
}
</style>
