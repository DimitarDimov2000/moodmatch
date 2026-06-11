import { describe, expect, it } from 'vitest';

import { groupTagsByCategory } from '@/components/tags/tag-utils';
import type { TagResponse } from '@/types/api';

describe('tag-utils', () => {
  it('groups tags by category in stable category order', () => {
    const tags: TagResponse[] = [
      {
        id: '2',
        name: 'Warm',
        category: 'TONE',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      {
        id: '1',
        name: 'Sci-Fi',
        category: 'GENRE',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      {
        id: '3',
        name: 'Alienation',
        category: 'THEME',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ];

    expect(groupTagsByCategory(tags)).toEqual([
      {
        category: 'GENRE',
        label: 'Genre',
        tags: [tags[1]],
      },
      {
        category: 'THEME',
        label: 'Thema',
        tags: [tags[2]],
      },
      {
        category: 'TONE',
        label: 'Ton',
        tags: [tags[0]],
      },
    ]);
  });
});
