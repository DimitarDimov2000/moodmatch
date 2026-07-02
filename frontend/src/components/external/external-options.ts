import type { MediaType } from '@/types/api-common';
import type { ExternalSearchResponseSourceName, ExternalSearchSourceName } from '@/types/external';

export interface ExternalOptionItem<T extends string> {
  value: T;
  label: string;
  disabled?: boolean;
}

export type ExternalSourceSelection = 'AUTO' | ExternalSearchSourceName;

export const externalSourceLabels: Record<ExternalSearchResponseSourceName, string> = {
  AUTOMATIC: 'Automatic',
  DEMO: 'DEMO',
  TMDB: 'TMDB',
  OPEN_LIBRARY: 'Open Library',
  RAWG: 'RAWG',
  LIBRIVOX: 'LibriVox',
  PODCAST_INDEX: 'Podcast Index',
  ANILIST: 'AniList',
  YOUTUBE: 'YouTube',
};

export function sourceOptionsForMediaType(
  mediaType: MediaType,
): ExternalOptionItem<ExternalSourceSelection>[] {
  switch (mediaType) {
    case 'FILM':
      return [
        { value: 'AUTO', label: 'Automatisch (TMDB + AniList)' },
        { value: 'TMDB', label: 'TMDB (Filme/Serien)' },
        { value: 'ANILIST', label: 'Anime movie / AniList' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'SERIES':
      return [
        { value: 'AUTO', label: 'Automatisch (TMDB + AniList)' },
        { value: 'TMDB', label: 'TMDB (Filme/Serien)' },
        { value: 'ANILIST', label: 'Anime series / AniList' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'BOOK':
      return [
        { value: 'AUTO', label: 'Automatisch (Open Library + AniList)' },
        { value: 'OPEN_LIBRARY', label: 'Open Library (Buecher)' },
        { value: 'ANILIST', label: 'Manga / AniList' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'GAME':
      return [
        { value: 'AUTO', label: 'Automatisch (RAWG, sonst DEMO)' },
        { value: 'RAWG', label: 'RAWG (Games)' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'AUDIOBOOK':
      return [
        { value: 'AUTO', label: 'Automatisch (LibriVox)' },
        { value: 'LIBRIVOX', label: 'LibriVox (Hoerbuecher)' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'PODCAST':
      return [
        { value: 'AUTO', label: 'Automatisch (Podcast Index)' },
        { value: 'PODCAST_INDEX', label: 'Podcast Index (Podcast-Shows)' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'VIDEO':
      return [
        { value: 'AUTO', label: 'Automatisch (noch keine aktive Suche)' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
  }
}

export function isSourceSelectionValid(
  mediaType: MediaType,
  selection: ExternalSourceSelection,
): boolean {
  return sourceOptionsForMediaType(mediaType).some((option) => option.value === selection && !option.disabled);
}

export function sourceHintForMediaType(mediaType: MediaType): string {
  switch (mediaType) {
    case 'FILM':
    case 'SERIES':
      return 'Automatisch durchsucht alle passenden Quellen: TMDB und AniList. Anime-Filme bleiben Film, Anime-Serien bleiben Serie.';
    case 'BOOK':
      return 'Automatisch durchsucht Open Library und AniList. Manga und Light Novels bleiben beim Import normale Buecher.';
    case 'GAME':
      return 'Automatisch durchsucht RAWG, wenn das Backend mit einem API-Key konfiguriert ist. Sonst faellt MoodMatch auf DEMO zurueck.';
    case 'AUDIOBOOK':
      return 'Automatisch durchsucht LibriVox. Der Katalog ist auf gemeinfreie Audiobooks begrenzt und benoetigt keinen geheimen API-Key.';
    case 'PODCAST':
      return 'Automatisch durchsucht Podcast Index fuer Podcast-Shows, wenn der Backend-Key und das Backend-Secret gesetzt sind. Einzelne Episoden werden nicht importiert.';
    case 'VIDEO':
      return 'Normale Videosuche bleibt ausserhalb des Scopes. Fuer YouTube-Videos nutze den separaten URL-Import unten.';
  }
}
