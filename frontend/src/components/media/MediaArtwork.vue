<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import {
  getMediaArtworkFallback,
  getMediaArtworkFallbackAlt,
  getMediaArtworkFallbackTitle,
  normalizeArtworkUrl,
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
    showFallbackTitle?: boolean;
  }>(),
  {
    coverUrl: null,
    variant: 'poster',
    altPrefix: '',
    showFallbackTitle: true,
  },
);

const imageReady = ref(false);
const imageFailed = ref(false);

const { t } = i18n.global;
const activeLocale = computed(() => i18n.global.locale.value);
const trackLocaleDependency = () => activeLocale.value;
const normalizedCoverUrl = computed(() => normalizeArtworkUrl(props.coverUrl));
const fallback = computed(() => {
  trackLocaleDependency();
  return getMediaArtworkFallback(props.mediaType, props.variant);
});
const altText = computed(() => {
  trackLocaleDependency();
  return props.altPrefix ? `${props.altPrefix} ${props.title}` : t('mediaArtwork.coverAlt', { title: props.title });
});
const fallbackAltText = computed(() => {
  trackLocaleDependency();
  return getMediaArtworkFallbackAlt(props.mediaType, props.title);
});
const fallbackTitle = computed(() => getMediaArtworkFallbackTitle(props.title));
const showFallbackTitle = computed(() => props.showFallbackTitle && fallbackTitle.value.length > 0);
const showImage = computed(() => Boolean(normalizedCoverUrl.value) && !imageFailed.value);
const showFallback = computed(
  () => !normalizedCoverUrl.value || !imageReady.value || imageFailed.value,
);
const accessibleFallback = computed(
  () => !normalizedCoverUrl.value || imageFailed.value,
);

watch(
  normalizedCoverUrl,
  () => {
    imageReady.value = false;
    imageFailed.value = false;
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
      :src="normalizedCoverUrl ?? undefined"
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
      :role="accessibleFallback ? 'img' : undefined"
      :aria-label="accessibleFallback ? fallbackAltText : undefined"
      :aria-hidden="accessibleFallback ? undefined : 'true'"
    >
      <div
        class="media-artwork__ambient"
        aria-hidden="true"
      >
        <span class="media-artwork__orb media-artwork__orb--primary" />
        <span class="media-artwork__orb media-artwork__orb--secondary" />
        <span class="media-artwork__ring media-artwork__ring--outer" />
        <span class="media-artwork__ring media-artwork__ring--inner" />
      </div>
      <div class="media-artwork__fallback-body">
        <div class="media-artwork__topline">
          <span class="media-artwork__label">{{ fallback.label }}</span>
          <span
            class="media-artwork__seal"
            aria-hidden="true"
          >
            MM
          </span>
        </div>
        <span
          v-if="showFallbackTitle"
          class="media-artwork__title"
        >
          {{ fallbackTitle }}
        </span>
        <span
          v-if="variant !== 'hero'"
          class="media-artwork__hint"
        >
          {{ fallback.hint }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.media-artwork {
  position: relative;
  isolation: isolate;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 14% 16%, rgba(255, 255, 255, 0.18), transparent 28%),
    radial-gradient(circle at 82% 18%, rgba(255, 255, 255, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 34%),
    var(--color-surface-secondary);
  box-shadow: var(--shadow-card);
}

.media-artwork::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.08), transparent 44%),
    radial-gradient(circle at bottom left, rgba(255, 255, 255, 0.12), transparent 36%),
    linear-gradient(120deg, rgba(255, 255, 255, 0.04), transparent 55%);
  opacity: 0.95;
  pointer-events: none;
}

.media-artwork::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 1.1rem 1.1rem;
  opacity: 0.06;
  pointer-events: none;
}

.media-artwork--poster {
  aspect-ratio: 4 / 5;
}

.media-artwork--landscape {
  aspect-ratio: 16 / 9;
}

.media-artwork--hero {
  aspect-ratio: auto;
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
  height: 100%;
  padding: 1rem 1rem 1.05rem;
  color: rgba(255, 255, 255, 0.94);
}

.media-artwork--landscape .media-artwork__fallback-copy {
  padding: 0.95rem 1.1rem 1rem;
}

.media-artwork--hero .media-artwork__fallback-copy {
  padding: 1rem;
}

.media-artwork__ambient {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.media-artwork__orb,
.media-artwork__ring {
  position: absolute;
  border-radius: 999px;
}

.media-artwork__orb {
  filter: blur(2px);
  opacity: 0.88;
}

.media-artwork__orb--primary {
  top: 0.9rem;
  right: 1rem;
  width: clamp(4rem, 18vw, 6rem);
  height: clamp(4rem, 18vw, 6rem);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.04) 66%, transparent 74%);
}

.media-artwork__orb--secondary {
  left: -1rem;
  bottom: -1.35rem;
  width: clamp(4.5rem, 20vw, 6.8rem);
  height: clamp(4.5rem, 20vw, 6.8rem);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.02) 70%, transparent 76%);
}

.media-artwork__ring {
  border: 1px solid rgba(255, 255, 255, 0.14);
  opacity: 0.72;
}

.media-artwork__ring--outer {
  right: -1.2rem;
  bottom: 1.4rem;
  width: clamp(4.6rem, 24vw, 7rem);
  height: clamp(4.6rem, 24vw, 7rem);
}

.media-artwork__ring--inner {
  left: 0.95rem;
  top: 0.95rem;
  width: clamp(2.5rem, 10vw, 3.8rem);
  height: clamp(2.5rem, 10vw, 3.8rem);
}

.media-artwork__fallback-body {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.42rem;
  max-width: min(100%, 14rem);
}

.media-artwork__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.55rem;
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

.media-artwork__seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.9rem;
  min-height: 1.9rem;
  padding: 0.2rem;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(8, 12, 24, 0.18);
  color: rgba(255, 255, 255, 0.88);
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.08em;
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
  text-wrap: balance;
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

.media-artwork--hero .media-artwork__fallback-body {
  max-width: min(100%, 11rem);
  gap: 0.35rem;
}

.media-artwork--hero .media-artwork__label {
  min-height: 1.6rem;
  padding-inline: 0.58rem;
  font-size: 0.7rem;
}

.media-artwork--hero .media-artwork__seal {
  min-width: 1.75rem;
  min-height: 1.75rem;
  font-size: 0.66rem;
}

.media-artwork--film {
  background:
    radial-gradient(circle at top right, rgba(255, 214, 153, 0.32), transparent 34%),
    radial-gradient(circle at 12% 76%, rgba(255, 248, 220, 0.18), transparent 28%),
    linear-gradient(135deg, rgba(255, 169, 64, 0.54), rgba(59, 130, 246, 0.2) 72%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--series {
  background:
    radial-gradient(circle at top right, rgba(158, 200, 255, 0.32), transparent 34%),
    radial-gradient(circle at 18% 82%, rgba(197, 214, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(59, 130, 246, 0.52), rgba(139, 124, 255, 0.24) 70%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--book {
  background:
    radial-gradient(circle at top right, rgba(182, 255, 214, 0.26), transparent 34%),
    radial-gradient(circle at 18% 80%, rgba(222, 255, 232, 0.16), transparent 22%),
    linear-gradient(135deg, rgba(22, 163, 74, 0.44), rgba(74, 222, 128, 0.18) 74%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--audiobook {
  background:
    radial-gradient(circle at top right, rgba(255, 194, 213, 0.3), transparent 34%),
    radial-gradient(circle at 16% 82%, rgba(255, 232, 239, 0.16), transparent 24%),
    linear-gradient(135deg, rgba(239, 68, 68, 0.42), rgba(255, 79, 135, 0.2) 72%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--game {
  background:
    radial-gradient(circle at top right, rgba(153, 255, 239, 0.26), transparent 34%),
    radial-gradient(circle at 14% 82%, rgba(225, 255, 245, 0.16), transparent 22%),
    linear-gradient(135deg, rgba(15, 118, 110, 0.5), rgba(34, 197, 94, 0.2) 70%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--podcast {
  background:
    radial-gradient(circle at top right, rgba(255, 221, 160, 0.3), transparent 34%),
    radial-gradient(circle at 15% 84%, rgba(255, 245, 217, 0.16), transparent 24%),
    linear-gradient(135deg, rgba(217, 119, 6, 0.48), rgba(251, 191, 36, 0.18) 72%),
    rgba(34, 43, 78, 0.78);
}

.media-artwork--video {
  background:
    radial-gradient(circle at top right, rgba(211, 194, 255, 0.32), transparent 34%),
    radial-gradient(circle at 18% 82%, rgba(236, 228, 255, 0.16), transparent 24%),
    linear-gradient(135deg, rgba(147, 51, 234, 0.5), rgba(59, 130, 246, 0.22) 72%),
    rgba(34, 43, 78, 0.78);
}

</style>
