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
        { value: 'YOUTUBE', label: 'YouTube (offizielle Videosuche)' },
      ];
  }
}

export function defaultSourceSelectionForMediaType(
  mediaType: MediaType,
): ExternalSourceSelection {
  return sourceOptionsForMediaType(mediaType)[0]?.value ?? 'AUTO';
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
      return 'Automatisch durchsucht alle passenden Provider fuer den gewaehlten Medientyp: TMDB und AniList. Anime-Filme bleiben Film, Anime-Serien bleiben Serie.';
    case 'BOOK':
      return 'Automatisch durchsucht alle passenden Provider fuer den gewaehlten Medientyp: Open Library und AniList. Manga und Light Novels bleiben beim Import normale Buecher.';
    case 'GAME':
      return 'Automatisch durchsucht alle passenden Provider fuer den gewaehlten Medientyp: RAWG. Wenn kein API-Key gesetzt ist, faellt MoodMatch auf DEMO zurueck.';
    case 'AUDIOBOOK':
      return 'Automatisch durchsucht alle passenden Provider fuer den gewaehlten Medientyp: LibriVox. Der Katalog ist auf gemeinfreie Audiobooks begrenzt.';
    case 'PODCAST':
      return 'Automatisch durchsucht alle passenden Provider fuer den gewaehlten Medientyp: Podcast Index. Einzelne Episoden werden nicht importiert.';
    case 'VIDEO':
      return 'YouTube-Suche nutzt die offizielle YouTube Data API fuer Video-Treffer. Fuer bekannte Links oder rohe IDs bleibt der separate YouTube-URL-Import darunter verfuegbar.';
  }
}
