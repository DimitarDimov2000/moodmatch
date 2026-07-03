<script setup lang="ts">
import { i18n } from '@/i18n';

const { t } = i18n.global;
</script>

<template>
  <section class="not-found page-card">
    <p class="eyebrow">
      {{ t('notFound.eyebrow') }}
    </p>
    <h1 class="section-title">
      {{ t('notFound.title') }}
    </h1>
    <p class="body-muted">
      {{ t('notFound.copy') }}
    </p>
  </section>
</template>

<style scoped>
.not-found {
  padding: 1.5rem;
}
</style>
