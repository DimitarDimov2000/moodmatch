import { afterEach, describe, expect, it, vi } from 'vitest';

import { API_BASE_URL } from '@/api/config';
import { importExternalMedia, resolveExternalUrl, searchExternal } from '@/api/external';

describe('external api', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('serializes external search query parameters onto the shared GET client', async () => {
    const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          query: 'dune',
          mediaType: 'BOOK',
          source: 'OPEN_LIBRARY',
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
      query: 'dune',
      mediaType: 'BOOK',
      source: 'OPEN_LIBRARY',
      limit: 3,
    });

    expect(fetchSpy).toHaveBeenCalledWith(
      new URL(
        `${API_BASE_URL}/external/search?query=dune&mediaType=BOOK&source=OPEN_LIBRARY&limit=3`,
        window.location.origin,
      ).toString(),
      expect.objectContaining({
        method: 'GET',
      }),
    );
  });

  it('serializes explicit youtube search sort parameters onto the shared GET client', async () => {
    const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          query: 'ai tutorial',
          mediaType: 'VIDEO',
          source: 'YOUTUBE',
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
      query: 'ai tutorial',
      mediaType: 'VIDEO',
      source: 'YOUTUBE',
      sort: 'most_viewed',
    });

    expect(fetchSpy).toHaveBeenCalledWith(
      new URL(
        `${API_BASE_URL}/external/search?query=ai+tutorial&mediaType=VIDEO&source=YOUTUBE&sort=most_viewed`,
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
      source: 'OPEN_LIBRARY',
      externalId: 'OL82563W',
      mediaType: 'BOOK',
      title: 'Dune',
      originalTitle: null,
      creatorNames: ['Frank Herbert'],
      description: 'Book by Frank Herbert. First published in 1965.',
      releaseYear: 1965,
      coverUrl: 'https://covers.openlibrary.org/b/id/987654-M.jpg',
      sourceUrl: 'https://openlibrary.org/works/OL82563W',
      externalGenres: [],
      externalSubjects: ['Politics'],
      attribution: 'Metadata from Open Library',
    });

    expect(fetchSpy).toHaveBeenCalledWith(
      new URL(`${API_BASE_URL}/external/import`, window.location.origin).toString(),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          source: 'OPEN_LIBRARY',
          externalId: 'OL82563W',
          mediaType: 'BOOK',
          title: 'Dune',
          originalTitle: null,
          creatorNames: ['Frank Herbert'],
          description: 'Book by Frank Herbert. First published in 1965.',
          releaseYear: 1965,
          coverUrl: 'https://covers.openlibrary.org/b/id/987654-M.jpg',
          sourceUrl: 'https://openlibrary.org/works/OL82563W',
          externalGenres: [],
          externalSubjects: ['Politics'],
          attribution: 'Metadata from Open Library',
        }),
      }),
    );
  });

  it('posts youtube resolve requests onto the shared JSON client', async () => {
    const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          source: 'YOUTUBE',
          externalId: 'abc123XYZ_0',
          mediaType: 'VIDEO',
          title: 'VueConf 2024 Keynote',
          originalTitle: null,
          creatorNames: ['MoodMatch Dev'],
          description: 'A practical keynote.',
          releaseYear: 2024,
          coverUrl: 'https://img.youtube.test/maxres.jpg',
          sourceUrl: 'https://www.youtube.com/watch?v=abc123XYZ_0',
          externalGenres: ['Education'],
          externalSubjects: ['Vue 3', 'Tutorial'],
          suggestedTags: [],
          attribution: 'Metadata from YouTube',
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

    await resolveExternalUrl({
      source: 'YOUTUBE',
      url: 'https://youtu.be/abc123XYZ_0',
    });

    expect(fetchSpy).toHaveBeenCalledWith(
      new URL(`${API_BASE_URL}/external/resolve-url`, window.location.origin).toString(),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          source: 'YOUTUBE',
          url: 'https://youtu.be/abc123XYZ_0',
        }),
      }),
    );
  });
});
