<script setup lang="ts">
import type { ExternalSearchResultResponse } from '@/types/api';

defineProps<{
  result: ExternalSearchResultResponse;
}>();
</script>

<template>
  <article class="external-result-card page-card">
    <div class="external-result-card__layout">
      <div class="external-result-card__copy">
        <div class="external-result-card__eyebrow-row">
          <span class="eyebrow">External Preview</span>
          <span class="external-result-card__source-badge">{{ result.source }}</span>
        </div>

        <h2 class="external-result-card__title">
          {{ result.title }}
        </h2>

        <p class="body-muted">
          {{ result.mediaType }}<span v-if="result.releaseYear"> • {{ result.releaseYear }}</span>
        </p>

        <p
          v-if="result.description"
          class="external-result-card__description"
        >
          {{ result.description }}
        </p>

        <div
          v-if="result.externalGenres.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            Externe Genres
          </p>
          <div class="external-result-card__chip-row">
            <span
              v-for="genre in result.externalGenres"
              :key="genre"
              class="external-result-card__chip external-result-card__chip--genre"
            >
              {{ genre }}
            </span>
          </div>
        </div>

        <div
          v-if="result.externalSubjects.length > 0"
          class="external-result-card__section"
        >
          <p class="external-result-card__section-label">
            Externe Subjects
          </p>
          <div class="external-result-card__chip-row">
            <span
              v-for="subject in result.externalSubjects"
              :key="subject"
              class="external-result-card__chip external-result-card__chip--subject"
            >
              {{ subject }}
            </span>
          </div>
        </div>

        <div class="external-result-card__section">
          <p class="external-result-card__section-label">
            Vorgeschlagene Tags
          </p>
          <p
            v-if="result.suggestedTags.length === 0"
            class="body-muted"
          >
            Fuer dieses Demo-Ergebnis liegen noch keine gemappten Tag-Vorschlaege vor.
          </p>
          <div
            v-else
            class="external-result-card__chip-row"
          >
            <span
              v-for="suggestedTag in result.suggestedTags"
              :key="`${suggestedTag.tagId}-${suggestedTag.sourceValue}`"
              class="external-result-card__chip external-result-card__chip--suggested"
            >
              {{ suggestedTag.tagName }} - {{ suggestedTag.confidence }}
            </span>
          </div>
        </div>

        <footer class="external-result-card__footer">
          <p class="body-muted">
            {{ result.attribution }}
          </p>
          <a
            v-if="result.sourceUrl"
            :href="result.sourceUrl"
            class="external-result-card__link"
            target="_blank"
            rel="noreferrer"
          >
            Detail-Quelle ansehen
          </a>
        </footer>
      </div>

      <div class="external-result-card__cover-frame">
        <img
          v-if="result.coverUrl"
          :src="result.coverUrl"
          :alt="`Cover von ${result.title}`"
          class="external-result-card__cover"
        >
        <div
          v-else
          class="external-result-card__cover external-result-card__cover--fallback"
        >
          Kein Cover
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.external-result-card {
  padding: 1.5rem;
}

.external-result-card__layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 1fr) 10rem;
}

.external-result-card__copy {
  display: grid;
  gap: 0.9rem;
}

.external-result-card__eyebrow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  align-items: center;
}

.external-result-card__source-badge {
  display: inline-flex;
  padding: 0.35rem 0.7rem;
  border-radius: var(--radius-full);
  background: var(--color-info-soft);
  color: var(--color-info);
  border: 1px solid color-mix(in srgb, var(--color-info) 24%, var(--color-border));
  font-size: 0.85rem;
  font-weight: 600;
}

.external-result-card__title,
.external-result-card__description,
.external-result-card__section-label {
  margin: 0;
}

.external-result-card__description {
  color: var(--color-text-secondary);
  line-height: 1.55;
}

.external-result-card__section {
  display: grid;
  gap: 0.55rem;
}

.external-result-card__section-label {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.external-result-card__chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.external-result-card__chip {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0.7rem;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.external-result-card__chip--genre {
  background: color-mix(in srgb, var(--color-info-soft) 80%, var(--color-surface));
}

.external-result-card__chip--subject {
  background: color-mix(in srgb, var(--color-warning-soft) 72%, var(--color-surface));
}

.external-result-card__chip--suggested {
  background: color-mix(in srgb, var(--color-success-soft) 78%, var(--color-surface));
  color: var(--color-success);
}

.external-result-card__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
  padding-top: 0.35rem;
}

.external-result-card__link {
  color: var(--color-accent-dark);
  font-weight: 600;
}

.external-result-card__cover-frame {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
}

.external-result-card__cover {
  width: 100%;
  max-width: 10rem;
  aspect-ratio: 4 / 5;
  object-fit: cover;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
}

.external-result-card__cover--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

@media (max-width: 820px) {
  .external-result-card__layout {
    grid-template-columns: 1fr;
  }

  .external-result-card__cover-frame {
    justify-content: flex-start;
  }
}
</style>
