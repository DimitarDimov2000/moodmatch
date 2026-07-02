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
});
