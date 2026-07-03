import { getExternalSourceLabel } from '@/components/media/media-presentation';
import { i18n } from '@/i18n';
import type { MediaType } from '@/types/api-common';
import type {
  ExternalSearchResponseSourceName,
  ExternalSearchSort,
  ExternalSearchSourceName,
} from '@/types/external';

export interface ExternalOptionItem<T extends string> {
  value: T;
  label: string;
  disabled?: boolean;
}

export type ExternalSourceSelection = 'AUTO' | ExternalSearchSourceName;

export function getExternalSourceLabels(): Record<ExternalSearchResponseSourceName, string> {
  return {
    AUTOMATIC: getExternalSourceLabel('AUTOMATIC'),
    DEMO: getExternalSourceLabel('DEMO'),
    TMDB: getExternalSourceLabel('TMDB'),
    OPEN_LIBRARY: getExternalSourceLabel('OPEN_LIBRARY'),
    RAWG: getExternalSourceLabel('RAWG'),
    LIBRIVOX: getExternalSourceLabel('LIBRIVOX'),
    PODCAST_INDEX: getExternalSourceLabel('PODCAST_INDEX'),
    ANILIST: getExternalSourceLabel('ANILIST'),
    YOUTUBE: getExternalSourceLabel('YOUTUBE'),
  };
}

export function getExternalSearchSortLabel(sort: ExternalSearchSort): string {
  const key = sort === 'most_viewed' ? 'mostViewed' : sort;
  return i18n.global.t(`externalSearch.sortOptions.${key}`);
}

export function getYouTubeSearchSortOptions(): ExternalOptionItem<ExternalSearchSort>[] {
  return [
    { value: 'relevance', label: getExternalSearchSortLabel('relevance') },
    { value: 'newest', label: getExternalSearchSortLabel('newest') },
    { value: 'most_viewed', label: getExternalSearchSortLabel('most_viewed') },
  ];
}

export const externalSourceLabels = new Proxy({} as Record<ExternalSearchResponseSourceName, string>, {
  get: (_, property) => {
    if (typeof property !== 'string' || property.startsWith('__v_')) {
      return undefined;
    }

    return getExternalSourceLabels()[property as ExternalSearchResponseSourceName];
  },
});

export const externalSearchSortLabels = new Proxy({} as Record<ExternalSearchSort, string>, {
  get: (_, property) => {
    if (typeof property !== 'string' || property.startsWith('__v_')) {
      return undefined;
    }

    return getExternalSearchSortLabel(property as ExternalSearchSort);
  },
});

export function sourceOptionsForMediaType(
  mediaType: MediaType,
): ExternalOptionItem<ExternalSourceSelection>[] {
  switch (mediaType) {
    case 'FILM':
      return [
        { value: 'AUTO', label: i18n.global.t('externalSearch.sourceOptions.autoFilmSeries') },
        { value: 'TMDB', label: i18n.global.t('externalSearch.sourceOptions.tmdbFilmSeries') },
        { value: 'ANILIST', label: i18n.global.t('externalSearch.sourceOptions.aniListFilm') },
        { value: 'DEMO', label: i18n.global.t('externalSearch.sourceOptions.demoFallback') },
      ];
    case 'SERIES':
      return [
        { value: 'AUTO', label: i18n.global.t('externalSearch.sourceOptions.autoFilmSeries') },
        { value: 'TMDB', label: i18n.global.t('externalSearch.sourceOptions.tmdbFilmSeries') },
        { value: 'ANILIST', label: i18n.global.t('externalSearch.sourceOptions.aniListSeries') },
        { value: 'DEMO', label: i18n.global.t('externalSearch.sourceOptions.demoFallback') },
      ];
    case 'BOOK':
      return [
        { value: 'AUTO', label: i18n.global.t('externalSearch.sourceOptions.autoBooks') },
        { value: 'OPEN_LIBRARY', label: i18n.global.t('externalSearch.sourceOptions.openLibraryBooks') },
        { value: 'ANILIST', label: i18n.global.t('externalSearch.sourceOptions.aniListManga') },
        { value: 'DEMO', label: i18n.global.t('externalSearch.sourceOptions.demoFallback') },
      ];
    case 'GAME':
      return [
        { value: 'AUTO', label: i18n.global.t('externalSearch.sourceOptions.autoGames') },
        { value: 'RAWG', label: i18n.global.t('externalSearch.sourceOptions.rawgGames') },
        { value: 'DEMO', label: i18n.global.t('externalSearch.sourceOptions.demoFallback') },
      ];
    case 'AUDIOBOOK':
      return [
        { value: 'AUTO', label: i18n.global.t('externalSearch.sourceOptions.autoAudiobooks') },
        { value: 'LIBRIVOX', label: i18n.global.t('externalSearch.sourceOptions.libriVoxAudiobooks') },
        { value: 'DEMO', label: i18n.global.t('externalSearch.sourceOptions.demoFallback') },
      ];
    case 'PODCAST':
      return [
        { value: 'AUTO', label: i18n.global.t('externalSearch.sourceOptions.autoPodcasts') },
        { value: 'PODCAST_INDEX', label: i18n.global.t('externalSearch.sourceOptions.podcastIndexShows') },
        { value: 'DEMO', label: i18n.global.t('externalSearch.sourceOptions.demoFallback') },
      ];
    case 'VIDEO':
      return [
        { value: 'YOUTUBE', label: i18n.global.t('externalSearch.sourceOptions.youtubeOfficial') },
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
  return i18n.global.t(`externalSearch.sourceHint.${mediaType}`);
}
