import { getMediaTypeLabel } from '@/components/media/media-options';
import { i18n } from '@/i18n';
import type { ExternalSourceName, MediaType } from '@/types/api';

export type DisplayableExternalSource = ExternalSourceName | 'AUTOMATIC';
export type MediaArtworkVariant = 'poster' | 'landscape';

interface MediaArtworkFallbackConfig {
  hint: string;
  accent: string;
}

export interface MediaArtworkFallback {
  initials: string;
  label: string;
  hint: string;
  accent: string;
}

const fallbackConfigByMediaType: Record<MediaType, MediaArtworkFallbackConfig> = {
  FILM: { hint: 'mediaArtwork.posterMissing', accent: 'film' },
  SERIES: { hint: 'mediaArtwork.posterMissing', accent: 'series' },
  BOOK: { hint: 'mediaArtwork.posterMissing', accent: 'book' },
  AUDIOBOOK: { hint: 'mediaArtwork.posterMissing', accent: 'audiobook' },
  GAME: { hint: 'mediaArtwork.artworkMissing', accent: 'game' },
  PODCAST: { hint: 'mediaArtwork.posterMissing', accent: 'podcast' },
  VIDEO: { hint: 'mediaArtwork.thumbnailMissing', accent: 'video' },
};

const htmlEntityPattern = /&(?:#\d+|#x[a-f0-9]+|[a-z][a-z0-9]+);/i;
let entityDecoder: HTMLTextAreaElement | null = null;

export function getExternalSourceLabel(
  source: DisplayableExternalSource | null | undefined,
): string {
  if (!source) {
    return '';
  }

  return i18n.global.t(`labels.provider.${source}`);
}

export function getDisplayText(value: string | null | undefined): string {
  if (typeof value !== 'string' || value.length === 0 || !htmlEntityPattern.test(value)) {
    return value ?? '';
  }

  if (typeof document !== 'undefined') {
    entityDecoder ??= document.createElement('textarea');
    entityDecoder.innerHTML = value;
    return entityDecoder.value.replace(/\u00a0/g, ' ');
  }

  return value
    .replace(/&#39;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&nbsp;/g, ' ');
}

export function getMediaArtworkFallback(
  mediaType: MediaType,
  title: string,
  variant: MediaArtworkVariant = 'poster',
): MediaArtworkFallback {
  const config = fallbackConfigByMediaType[mediaType];
  const initials = getTitleInitials(title);

  return {
    initials,
    label: getMediaTypeLabel(mediaType),
    hint:
      variant === 'landscape' && mediaType === 'VIDEO'
        ? i18n.global.t('mediaArtwork.previewComing')
        : i18n.global.t(config.hint),
    accent: config.accent,
  };
}

function getTitleInitials(title: string): string {
  const normalizedTitle = getDisplayText(title);
  const tokens = normalizedTitle
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2);

  if (tokens.length === 0) {
    return 'MM';
  }

  const initials = tokens
    .map((token) => token.replace(/^[^A-Za-z0-9]+|[^A-Za-z0-9]+$/g, '').slice(0, 1))
    .join('')
    .toUpperCase();

  return initials || normalizedTitle.slice(0, 2).toUpperCase() || 'MM';
}
