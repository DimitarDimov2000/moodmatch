import { getJson } from './client';
import type { CandidateSelectionResponse } from '@/types/api';

export function listCandidates(): Promise<CandidateSelectionResponse> {
  return getJson<CandidateSelectionResponse>('/candidates');
}
