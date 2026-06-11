import { getJson } from './client';
import type { InterestProfileResponse } from '@/types/api';

export function getProfile(): Promise<InterestProfileResponse> {
  return getJson<InterestProfileResponse>('/profile');
}
