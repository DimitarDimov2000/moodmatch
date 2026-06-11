const defaultApiBaseUrl = '/api';

function ensureApiSuffix(pathname: string): string {
  const trimmedPathname = pathname.replace(/\/+$/, '');

  if (!trimmedPathname || trimmedPathname === '/') {
    return defaultApiBaseUrl;
  }

  if (trimmedPathname === '/api' || trimmedPathname.endsWith('/api')) {
    return trimmedPathname;
  }

  return `${trimmedPathname}/api`;
}

export function normalizeApiBaseUrl(value: string | undefined): string {
  if (!value) {
    return defaultApiBaseUrl;
  }

  const trimmedValue = value.trim();
  if (!trimmedValue) {
    return defaultApiBaseUrl;
  }

  if (/^https?:\/\//i.test(trimmedValue)) {
    const url = new URL(trimmedValue);
    url.pathname = ensureApiSuffix(url.pathname);
    return url.toString().replace(/\/+$/, '');
  }

  const normalizedPath = trimmedValue.startsWith('/') ? trimmedValue : `/${trimmedValue}`;
  return ensureApiSuffix(normalizedPath);
}

export const API_BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);
