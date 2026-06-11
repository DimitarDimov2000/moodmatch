import { afterEach, describe, expect, it, vi } from 'vitest';

import { API_BASE_URL } from '@/api/config';
import { searchExternal } from '@/api/external';

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
});
