import { describe, expect, it } from 'vitest';

import {
  canBeFavourite,
  getMediaSubtitle,
} from '@/components/media/media-options';
import type { MediaResponse } from '@/types/api';

describe('media-options', () => {
  it('allows favourites only for consumed media with rating 4 or higher', () => {
    expect(canBeFavourite('CONSUMED', 4)).toBe(true);
    expect(canBeFavourite('CONSUMED', 5)).toBe(true);
    expect(canBeFavourite('CONSUMED', 3)).toBe(false);
    expect(canBeFavourite('WANT_TO_CONSUME', 5)).toBe(false);
    expect(canBeFavourite('CONSUMED', null)).toBe(false);
  });

  it('builds a readable subtitle for media cards', () => {
    const media: MediaResponse = {
      id: 'm1',
      title: 'Arrival',
      originalTitle: null,
      description: null,
      mediaType: 'FILM',
      consumptionStatus: 'CONSUMED',
      isFavourite: false,
      rating: 5,
      sourceType: 'MANUAL',
      sourceNote: null,
      commitmentLevel: 'MEDIUM',
      releaseYear: 2016,
      coverUrl: null,
      externalSourceName: null,
      externalSourceId: null,
      externalSourceUrl: null,
      metadataOrigin: 'MANUAL',
      tags: [],
      externalReferences: [],
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    expect(getMediaSubtitle(media)).toBe('Film • Konsumiert • 2016');
  });
});

