const defaultApiBaseUrl = '/api';

function normalizeApiBaseUrl(value: string | undefined): string {
  if (!value) {
    return defaultApiBaseUrl;
  }

  const trimmedValue = value.trim();
  if (!trimmedValue) {
    return defaultApiBaseUrl;
  }

  return trimmedValue.replace(/\/+$/, '') || defaultApiBaseUrl;
}

export const API_BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);
