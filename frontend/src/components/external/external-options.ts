import type { MediaType } from '@/types/api-common';
import type { ExternalSearchSourceName } from '@/types/external';

export interface ExternalOptionItem<T extends string> {
  value: T;
  label: string;
  disabled?: boolean;
}

export type ExternalSourceSelection = 'AUTO' | ExternalSearchSourceName;

export const externalSourceLabels: Record<ExternalSearchSourceName, string> = {
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
    case 'SERIES':
      return [
        { value: 'AUTO', label: 'Automatisch (TMDB, sonst DEMO)' },
        { value: 'TMDB', label: 'TMDB (Filme/Serien)' },
        { value: 'ANILIST', label: 'AniList (Anime geplant)', disabled: true },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'BOOK':
      return [
        { value: 'AUTO', label: 'Automatisch (Open Library)' },
        { value: 'OPEN_LIBRARY', label: 'Open Library (Buecher)' },
        { value: 'ANILIST', label: 'AniList (Manga geplant)', disabled: true },
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
        { value: 'AUTO', label: 'Automatisch (DEMO)' },
        { value: 'PODCAST_INDEX', label: 'Podcast Index (Podcasts geplant)', disabled: true },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'VIDEO':
      return [
        { value: 'AUTO', label: 'Automatisch (DEMO)' },
        { value: 'YOUTUBE', label: 'YouTube (URL-Import geplant)', disabled: true },
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
      return 'Filme und Serien bevorzugen TMDB, wenn das Backend mit einem API-Key konfiguriert ist. Sonst faellt MoodMatch auf DEMO zurueck.';
    case 'BOOK':
      return 'Buecher werden ueber Open Library gesucht. Dafuer ist kein geheimer API-Key noetig.';
    case 'GAME':
      return 'Games bevorzugen RAWG, wenn das Backend mit einem API-Key konfiguriert ist. Sonst faellt MoodMatch auf DEMO zurueck.';
    case 'AUDIOBOOK':
      return 'Hoerbuecher werden ueber LibriVox gesucht. Der Katalog ist auf gemeinfreie Audiobooks begrenzt und benoetigt keinen geheimen API-Key.';
    case 'PODCAST':
      return 'Podcast Index ist als Podcast-Quelle geplant. Episodenimport ist nicht Teil dieses Pakets.';
    case 'VIDEO':
      return 'YouTube ist nur fuer spaeteren URL-Import vorgesehen. YouTube-Suche bleibt ausserhalb des Scopes.';
  }
}
