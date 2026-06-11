import { describe, expect, it } from 'vitest';

import { normalizeApiBaseUrl } from '@/api/config';

describe('api config', () => {
  it('defaults to the shared /api prefix when the env value is missing', () => {
    expect(normalizeApiBaseUrl(undefined)).toBe('/api');
    expect(normalizeApiBaseUrl('')).toBe('/api');
    expect(normalizeApiBaseUrl('   ')).toBe('/api');
  });

  it('keeps relative paths on the /api prefix', () => {
    expect(normalizeApiBaseUrl('/api')).toBe('/api');
    expect(normalizeApiBaseUrl('/api/')).toBe('/api');
    expect(normalizeApiBaseUrl('api')).toBe('/api');
    expect(normalizeApiBaseUrl('/backend')).toBe('/backend/api');
  });

  it('appends /api to absolute backend urls when needed', () => {
    expect(normalizeApiBaseUrl('http://localhost:8080')).toBe('http://localhost:8080/api');
    expect(normalizeApiBaseUrl('http://localhost:8080/')).toBe('http://localhost:8080/api');
    expect(normalizeApiBaseUrl('http://localhost:8080/api')).toBe('http://localhost:8080/api');
  });
});
