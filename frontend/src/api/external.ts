import { getJson, postJson } from './client';
import type {
  ExternalImportRequest,
  ExternalImportResponse,
  ExternalSearchQuery,
  ExternalSearchResponse,
} from '@/types/api';

export function searchExternal(query: ExternalSearchQuery): Promise<ExternalSearchResponse> {
  return getJson<ExternalSearchResponse>('/external/search', {
    query: {
      query: query.query,
      mediaType: query.mediaType,
      source: query.source,
      limit: query.limit,
    },
  });
}

export function importExternalMedia(request: ExternalImportRequest): Promise<ExternalImportResponse> {
  return postJson<ExternalImportResponse, ExternalImportRequest>('/external/import', request);
}
