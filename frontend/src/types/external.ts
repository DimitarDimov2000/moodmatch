import type { MediaType, TagCategory } from './api-common';
import type { MediaResponse } from './media';

export type ExternalSearchSourceName = 'DEMO' | 'TMDB';

export interface ExternalSearchQuery {
  query: string;
  mediaType: MediaType;
  source?: ExternalSearchSourceName;
  limit?: number;
}

export interface ExternalSuggestedTagResponse {
  tagId: string;
  tagName: string;
  tagCategory: TagCategory;
  sourceValue: string;
  reason: string;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface ExternalSearchResultResponse {
  source: ExternalSearchSourceName;
  externalId: string;
  mediaType: MediaType;
  title: string;
  originalTitle: string | null;
  description: string | null;
  releaseYear: number | null;
  coverUrl: string | null;
  sourceUrl: string | null;
  externalGenres: string[];
  externalSubjects: string[];
  suggestedTags: ExternalSuggestedTagResponse[];
  attribution: string;
  warnings: string[];
}

export interface ExternalSearchResponse {
  query: string;
  mediaType: MediaType;
  source: ExternalSearchSourceName;
  results: ExternalSearchResultResponse[];
  warnings: string[];
}

export interface ExternalImportRequest {
  source: ExternalSearchSourceName;
  externalId: string;
  mediaType: MediaType;
  title: string;
  originalTitle: string | null;
  description: string | null;
  releaseYear: number | null;
  coverUrl: string | null;
  sourceUrl: string | null;
  externalGenres: string[];
  externalSubjects: string[];
  attribution: string | null;
}

export interface ExternalImportResponse {
  media: MediaResponse;
  created: boolean;
  message: string;
}
