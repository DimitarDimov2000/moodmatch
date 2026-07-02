export type MediaType = 'FILM' | 'SERIES' | 'BOOK' | 'GAME' | 'PODCAST' | 'AUDIOBOOK' | 'VIDEO';

export type ConsumptionStatus =
  | 'CONSUMED'
  | 'WANT_TO_CONSUME'
  | 'NOT_INTERESTED'
  | 'ABANDONED';

export type CommitmentLevel = 'SHORT' | 'MEDIUM' | 'LONG' | 'UNKNOWN';

export type SourceType =
  | 'FRIEND'
  | 'SOCIAL_MEDIA'
  | 'ARTICLE'
  | 'PLATFORM'
  | 'MANUAL'
  | 'EXTERNAL_SEARCH'
  | 'UNKNOWN';

export type MetadataOrigin = 'MANUAL' | 'IMPORTED' | 'IMPORTED_AND_EDITED';

export type TagCategory = 'GENRE' | 'THEME' | 'SETTING' | 'TONE' | 'EXPERIENCE';

export type ExternalSourceName =
  | 'DEMO'
  | 'TMDB'
  | 'OPEN_LIBRARY'
  | 'RAWG'
  | 'LIBRIVOX'
  | 'PODCAST_INDEX'
  | 'ANILIST'
  | 'YOUTUBE'
  | 'WIKIDATA'
  | 'IGDB'
  | 'GOOGLE_BOOKS'
  | 'TVMAZE';

export interface ApiErrorDetail {
  field: string | null;
  message: string;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  details: ApiErrorDetail[];
}
