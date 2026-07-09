<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import {
  getMediaArtworkFallback,
  type MediaArtworkVariant,
} from '@/components/media/media-presentation';
import { i18n } from '@/i18n';
import type { MediaType } from '@/types/api';

const props = withDefaults(
  defineProps<{
    title: string;
    mediaType: MediaType;
    coverUrl?: string | null;
    variant?: MediaArtworkVariant;
    altPrefix?: string;
  }>(),
  {
    coverUrl: null,
    variant: 'poster',
    altPrefix: '',
  },
);

const imageReady = ref(false);
const imageFailed = ref(false);

const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;
const fallback = computed(() => {
  trackLocaleDependency();
  return getMediaArtworkFallback(props.mediaType, props.title, props.variant);
});
const altText = computed(() => {
  trackLocaleDependency();
  return props.altPrefix ? `${props.altPrefix} ${props.title}` : t('mediaArtwork.coverAlt', { title: props.title });
});
const showImage = computed(() => Boolean(props.coverUrl) && !imageFailed.value);
const showFallback = computed(
  () => !props.coverUrl || !imageReady.value || imageFailed.value,
);

watch(
  () => props.coverUrl,
  (value) => {
    imageReady.value = false;
    imageFailed.value = false;

    if (!value) {
      imageReady.value = true;
    }
  },
  { immediate: true },
);

function handleLoad() {
  imageReady.value = true;
}

function handleError() {
  imageFailed.value = true;
  imageReady.value = true;
}
</script>

<template>
  <div
    class="media-artwork"
    :class="[
      `media-artwork--${variant}`,
      `media-artwork--${fallback.accent}`,
      {
        'media-artwork--fallback': showFallback,
      },
    ]"
  >
    <img
      v-if="showImage"
      :src="coverUrl ?? undefined"
      :alt="altText"
      class="media-artwork__image"
      :class="{ 'media-artwork__image--ready': imageReady }"
      loading="lazy"
      decoding="async"
      @load="handleLoad"
      @error="handleError"
    >

    <div
      v-if="showFallback"
      class="media-artwork__fallback-copy"
      :aria-label="t('mediaArtwork.placeholder', { label: fallback.label })"
    >
      <span
        class="media-artwork__monogram"
        aria-hidden="true"
      >
        {{ fallback.initials }}
      </span>
      <div class="media-artwork__fallback-body">
        <span class="media-artwork__label">{{ fallback.label }}</span>
        <span class="media-artwork__title">{{ title }}</span>
        <span class="media-artwork__hint">{{ fallback.hint }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.media-artwork {
  position: relative;
  isolation: isolate;
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 34%),
    var(--color-surface-secondary);
  box-shadow: var(--shadow-card);
}

.media-artwork::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 44%),
    radial-gradient(circle at bottom left, rgba(255, 255, 255, 0.12), transparent 36%);
  opacity: 0.9;
  pointer-events: none;
}

.media-artwork--poster {
  aspect-ratio: 4 / 5;
}

.media-artwork--landscape {
  aspect-ratio: 16 / 9;
}

.media-artwork__image,
.media-artwork__fallback-copy {
  position: relative;
  z-index: 1;
}

.media-artwork__image {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 180ms ease;
}

.media-artwork__image--ready {
  opacity: 1;
}

.media-artwork__fallback-copy {
  position: relative;
  display: grid;
  align-content: end;
  justify-items: start;
  gap: 0.5rem;
  height: 100%;
  padding: 1rem 1rem 1.05rem;
  color: rgba(255, 255, 255, 0.94);
}

.media-artwork--landscape .media-artwork__fallback-copy {
  padding: 0.95rem 1.1rem 1rem;
}

.media-artwork__fallback-body {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.28rem;
  max-width: min(100%, 14rem);
}

.media-artwork__monogram {
  position: absolute;
  right: 0.8rem;
  bottom: 0.55rem;
  z-index: 0;
  color: rgba(255, 255, 255, 0.16);
  font-size: clamp(2.5rem, 9vw, 4.2rem);
  font-weight: 800;
  letter-spacing: -0.08em;
  line-height: 0.82;
  pointer-events: none;
  user-select: none;
}

.media-artwork__label {
  display: inline-flex;
  align-items: center;
  min-height: 1.7rem;
  padding: 0.2rem 0.6rem;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: var(--radius-full);
  background: rgba(10, 16, 36, 0.28);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.media-artwork__title {
  display: -webkit-box;
  overflow: hidden;
  font-size: clamp(1rem, 3vw, 1.42rem);
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.08;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.media-artwork__hint {
  max-width: 18ch;
  color: rgba(255, 255, 255, 0.76);
  font-size: 0.78rem;
  line-height: 1.3;
}

.media-artwork--landscape .media-artwork__fallback-body {
  max-width: min(100%, 22rem);
}

.media-artwork--landscape .media-artwork__title {
  font-size: clamp(0.98rem, 2vw, 1.2rem);
}

.media-artwork--landscape .media-artwork__hint {
  max-width: 24ch;
}

.media-artwork--landscape .media-artwork__monogram {
  font-size: clamp(2.1rem, 7vw, 3.5rem);
}

.media-artwork--film {
  background:
    radial-gradient(circle at top right, rgba(255, 214, 153, 0.32), transparent 34%),
    linear-gradient(135deg, rgba(255, 169, 64, 0.54), rgba(59, 130, 246, 0.2) 72%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--series {
  background:
    radial-gradient(circle at top right, rgba(158, 200, 255, 0.32), transparent 34%),
    linear-gradient(135deg, rgba(59, 130, 246, 0.52), rgba(139, 124, 255, 0.24) 70%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--book {
  background:
    radial-gradient(circle at top right, rgba(182, 255, 214, 0.26), transparent 34%),
    linear-gradient(135deg, rgba(22, 163, 74, 0.44), rgba(74, 222, 128, 0.18) 74%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--audiobook {
  background:
    radial-gradient(circle at top right, rgba(255, 194, 213, 0.3), transparent 34%),
    linear-gradient(135deg, rgba(239, 68, 68, 0.42), rgba(255, 79, 135, 0.2) 72%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--game {
  background:
    radial-gradient(circle at top right, rgba(153, 255, 239, 0.26), transparent 34%),
    linear-gradient(135deg, rgba(15, 118, 110, 0.5), rgba(34, 197, 94, 0.2) 70%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--podcast {
  background:
    radial-gradient(circle at top right, rgba(255, 221, 160, 0.3), transparent 34%),
    linear-gradient(135deg, rgba(217, 119, 6, 0.48), rgba(251, 191, 36, 0.18) 72%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--video {
  background:
    radial-gradient(circle at top right, rgba(211, 194, 255, 0.32), transparent 34%),
    linear-gradient(135deg, rgba(147, 51, 234, 0.5), rgba(59, 130, 246, 0.22) 72%),
    rgba(34, 43, 78, 0.78);
}

</style>
