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

    <RouterLink
      :to="linkTo"
      class="dashboard-summary-card__link"
    >
      {{ linkLabel }}
    </RouterLink>
  </article>
</template>

<style scoped>
.dashboard-summary-card {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 0.8rem;
  height: 100%;
  padding: 1.05rem;
  align-content: start;
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
  display: grid;
  gap: 0.25rem;
}

.dashboard-summary-card__body {
  display: grid;
  gap: 0.3rem;
}

.dashboard-summary-card__value,
.dashboard-summary-card__body p {
  margin: 0;
}

.dashboard-summary-card__value {
  font-size: clamp(1.75rem, 3vw, 2.25rem);
  line-height: 1;
}

.dashboard-summary-card__link {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  margin-top: 0.1rem;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
  font-weight: 700;
}

.dashboard-summary-card__link:hover {
  color: var(--color-text-primary);
}
</style>
