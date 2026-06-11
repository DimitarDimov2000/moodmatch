import { afterEach, describe, expect, it, vi } from 'vitest';

import { API_BASE_URL } from '@/api/config';
import { apiRequest, deleteRequest, getJson, patchJson } from '@/api/client';

describe('api client', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('builds requests with the shared base url and json headers', async () => {
    const fetchSpy = vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: {
          'Content-Type': 'application/json',
        },
      }),
    );

    const result = await apiRequest<{ ok: boolean }>('/media', {
      method: 'POST',
      body: { title: 'Interstellar' },
      query: {
        page: 2,
        includeArchived: false,
        empty: null,
      },
    });

    expect(result).toEqual({ ok: true });
    expect(fetchSpy).toHaveBeenCalledWith(
      `${API_BASE_URL}/media?page=2&includeArchived=false`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ title: 'Interstellar' }),
        headers: expect.any(Headers),
      }),
    );

    const [, requestInit] = fetchSpy.mock.calls[0] ?? [];
    const headers = requestInit?.headers as Headers;

    expect(headers.get('Accept')).toBe('application/json');
    expect(headers.get('Content-Type')).toBe('application/json');
  });

  it('maps backend error responses into ApiRequestError', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 'VALIDATION_ERROR',
          message: 'Validation failed.',
          details: [{ field: 'rating', message: 'Rating is required.' }],
        }),
        {
          status: 400,
          headers: {
            'Content-Type': 'application/json',
          },
        },
      ),
    );

    await expect(getJson('/profile')).rejects.toMatchObject({
      name: 'ApiRequestError',
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Validation failed.',
      details: [{ field: 'rating', message: 'Rating is required.' }],
    });
  });

  it('returns void for successful delete requests with no content', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(new Response(null, { status: 204 }));

    await expect(deleteRequest('/media/123')).resolves.toBeUndefined();
  });

  it('uses a request fallback when the backend error body is missing', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response('bad request', {
        status: 400,
        headers: {
          'Content-Type': 'text/plain',
        },
      }),
    );

    await expect(
      patchJson('/media/123/status', { consumptionStatus: 'ABANDONED' }),
    ).rejects.toMatchObject({
        status: 400,
        code: 'REQUEST_ERROR',
        message: 'Request failed with status 400.',
      });
  });

  it('surfaces network failures with a dedicated error code', async () => {
    vi.spyOn(window, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(getJson('/matches')).rejects.toMatchObject({
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Network request failed.',
    });
  });
});
