import { getJson } from './client';
import type { MatchingResponse } from '@/types/api';

export function getMatches(): Promise<MatchingResponse> {
  return getJson<MatchingResponse>('/matches');
}
