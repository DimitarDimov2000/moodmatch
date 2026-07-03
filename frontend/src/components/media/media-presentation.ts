import { mediaTypeLabels } from '@/components/media/media-options';
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

export const externalSourceDisplayLabels: Record<DisplayableExternalSource, string> = {
  AUTOMATIC: 'Automatisch',
  DEMO: 'Demo',
  TMDB: 'TMDB',
  OPEN_LIBRARY: 'Open Library',
  RAWG: 'RAWG',
  LIBRIVOX: 'LibriVox',
  PODCAST_INDEX: 'Podcast Index',
  ANILIST: 'AniList',
  YOUTUBE: 'YouTube',
  WIKIDATA: 'Wikidata',
  IGDB: 'IGDB',
  GOOGLE_BOOKS: 'Google Books',
  TVMAZE: 'TVMaze',
};

const fallbackConfigByMediaType: Record<MediaType, MediaArtworkFallbackConfig> = {
  FILM: { hint: 'Poster fehlt', accent: 'film' },
  SERIES: { hint: 'Cover fehlt', accent: 'series' },
  BOOK: { hint: 'Cover fehlt', accent: 'book' },
  AUDIOBOOK: { hint: 'Cover fehlt', accent: 'audiobook' },
  GAME: { hint: 'Artwork fehlt', accent: 'game' },
  PODCAST: { hint: 'Cover fehlt', accent: 'podcast' },
  VIDEO: { hint: 'Thumbnail fehlt', accent: 'video' },
};

export function getExternalSourceLabel(
  source: DisplayableExternalSource | null | undefined,
): string {
  if (!source) {
    return '';
  }

  return externalSourceDisplayLabels[source];
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
    label: mediaTypeLabels[mediaType],
    hint: variant === 'landscape' && mediaType === 'VIDEO' ? 'Vorschau folgt' : config.hint,
    accent: config.accent,
  };
}

function getTitleInitials(title: string): string {
  const tokens = title
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

  return initials || title.slice(0, 2).toUpperCase() || 'MM';
}
