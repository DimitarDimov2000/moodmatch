import { getJson, postJson } from './client';
import type {
  ExternalResolveUrlRequest,
  ExternalImportRequest,
  ExternalImportResponse,
  ExternalSearchQuery,
  ExternalSearchResponse,
  ExternalSearchResultResponse,
} from '@/types/api';

export function searchExternal(query: ExternalSearchQuery): Promise<ExternalSearchResponse> {
  return getJson<ExternalSearchResponse>('/external/search', {
    query: {
      query: query.query,
      mediaType: query.mediaType,
      source: query.source,
      limit: query.limit,
      sort: query.sort,
    },
  });
}

export function importExternalMedia(request: ExternalImportRequest): Promise<ExternalImportResponse> {
  return postJson<ExternalImportResponse, ExternalImportRequest>('/external/import', request);
}

export function resolveExternalUrl(request: ExternalResolveUrlRequest): Promise<ExternalSearchResultResponse> {
  return postJson<ExternalSearchResultResponse, ExternalResolveUrlRequest>('/external/resolve-url', request);
}
