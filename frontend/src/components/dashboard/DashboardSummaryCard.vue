<script setup lang="ts">
import { RouterLink } from "vue-router";

withDefaults(
  defineProps<{
    eyebrow: string;
    title: string;
    value: string;
    description: string;
    linkTo: { name: string };
    linkLabel: string;
    tone?: "default" | "info" | "success" | "warning";
  }>(),
  {
    tone: "default",
  },
);
</script>

<template>
  <article
    class="dashboard-summary-card page-card"
    :class="`dashboard-summary-card--${tone}`"
  >
    <div class="dashboard-summary-card__header">
      <p class="eyebrow">
        {{ eyebrow }}
      </p>
      <RouterLink
        :to="linkTo"
        class="dashboard-summary-card__link"
      >
        {{ linkLabel }}
      </RouterLink>
    </div>

    <div class="dashboard-summary-card__body">
      <h2 class="dashboard-summary-card__value">
        {{ value }}
      </h2>
      <h3 class="section-title">
        {{ title }}
      </h3>
      <p class="body-muted">
        {{ description }}
      </p>
    </div>
  </article>
</template>

<style scoped>
.dashboard-summary-card {
  display: grid;
  gap: 1rem;
  padding: 1.35rem;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--color-surface) 92%, transparent),
    var(--color-surface)
  );
}

.dashboard-summary-card--info {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--color-info-soft) 55%, var(--color-surface)),
    var(--color-surface)
  );
}

.dashboard-summary-card--success {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--color-success-soft) 65%, var(--color-surface)),
    var(--color-surface)
  );
}

.dashboard-summary-card--warning {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--color-warning-soft) 68%, var(--color-surface)),
    var(--color-surface)
  );
}

.dashboard-summary-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.dashboard-summary-card__body {
  display: grid;
  gap: 0.35rem;
}

.dashboard-summary-card__value,
.dashboard-summary-card__body p {
  margin: 0;
}

.dashboard-summary-card__value {
  font-size: clamp(2rem, 4vw, 2.6rem);
  line-height: 1;
}

.dashboard-summary-card__link {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.25rem 0.7rem;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  font-weight: 600;
}

.dashboard-summary-card__link:hover {
  border-color: var(--color-border-strong);
  color: var(--color-text-primary);
}
</style>
