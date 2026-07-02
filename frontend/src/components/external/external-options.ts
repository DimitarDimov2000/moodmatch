import type { MediaType } from '@/types/api-common';
import type { ExternalSearchSourceName } from '@/types/external';

export interface ExternalOptionItem<T extends string> {
  value: T;
  label: string;
}

export type ExternalSourceSelection = 'AUTO' | ExternalSearchSourceName;

export const externalSourceLabels: Record<ExternalSearchSourceName, string> = {
  DEMO: 'DEMO',
  TMDB: 'TMDB',
  OPEN_LIBRARY: 'Open Library',
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
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'BOOK':
      return [
        { value: 'AUTO', label: 'Automatisch (Open Library)' },
        { value: 'OPEN_LIBRARY', label: 'Open Library (Buecher)' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
    case 'GAME':
      return [
        { value: 'AUTO', label: 'Automatisch (DEMO)' },
        { value: 'DEMO', label: 'DEMO-Fallback' },
      ];
  }
}

export function isSourceSelectionValid(
  mediaType: MediaType,
  selection: ExternalSourceSelection,
): boolean {
  return sourceOptionsForMediaType(mediaType).some((option) => option.value === selection);
}

export function sourceHintForMediaType(mediaType: MediaType): string {
  switch (mediaType) {
    case 'FILM':
    case 'SERIES':
      return 'Filme und Serien bevorzugen TMDB, wenn das Backend mit einem API-Key konfiguriert ist. Sonst faellt MoodMatch auf DEMO zurueck.';
    case 'BOOK':
      return 'Buecher werden ueber Open Library gesucht. Dafuer ist kein geheimer API-Key noetig.';
    case 'GAME':
      return 'Games bleiben in Package 2 beim DEMO-Fallback.';
  }
}
