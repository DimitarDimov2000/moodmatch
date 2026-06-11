import { getJson } from './client';
import type { TagResponse } from '@/types/api';

export function listTags(): Promise<TagResponse[]> {
  return getJson<TagResponse[]>('/tags');
}
