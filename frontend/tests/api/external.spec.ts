import { afterEach, describe, expect, it, vi } from 'vitest';

import { API_BASE_URL } from '@/api/config';
import { importExternalMedia, searchExternal } from '@/api/external';

describe('external api', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('serializes external search query parameters onto the shared GET client', async () => {
    const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          query: 'arrival',
          mediaType: 'FILM',
          source: 'DEMO',
          results: [],
          warnings: [],
        }),
        {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
        },
      ),
    );

    await searchExternal({
      query: 'arrival',
      mediaType: 'FILM',
      source: 'DEMO',
      limit: 3,
    });

    expect(fetchSpy).toHaveBeenCalledWith(
      new URL(
        `${API_BASE_URL}/external/search?query=arrival&mediaType=FILM&source=DEMO&limit=3`,
        window.location.origin,
      ).toString(),
      expect.objectContaining({
        method: 'GET',
      }),
    );
  });

  it('posts import payloads onto the shared JSON client', async () => {
    const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          created: true,
          message: 'Imported into your media library.',
          media: { id: 'media-1' },
        }),
        {
          status: 201,
          headers: {
            'Content-Type': 'application/json',
          },
        },
      ),
    );

    await importExternalMedia({
      source: 'TMDB',
      externalId: '11',
      mediaType: 'FILM',
      title: 'Arrival',
      originalTitle: null,
      description: 'First contact changes everything.',
      releaseYear: 2016,
      coverUrl: 'https://image.tmdb.org/t/p/w342/poster.jpg',
      sourceUrl: 'https://www.themoviedb.org/movie/11',
      externalGenres: ['Science Fiction'],
      externalSubjects: [],
      attribution: 'Metadata from TMDB',
    });

    expect(fetchSpy).toHaveBeenCalledWith(
      new URL(`${API_BASE_URL}/external/import`, window.location.origin).toString(),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          source: 'TMDB',
          externalId: '11',
          mediaType: 'FILM',
          title: 'Arrival',
          originalTitle: null,
          description: 'First contact changes everything.',
          releaseYear: 2016,
          coverUrl: 'https://image.tmdb.org/t/p/w342/poster.jpg',
          sourceUrl: 'https://www.themoviedb.org/movie/11',
          externalGenres: ['Science Fiction'],
          externalSubjects: [],
          attribution: 'Metadata from TMDB',
        }),
      }),
    );
  });
});
