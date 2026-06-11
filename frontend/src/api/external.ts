import { getJson } from './client';
import type { ExternalSearchQuery, ExternalSearchResponse } from '@/types/api';

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
